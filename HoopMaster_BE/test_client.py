#!/usr/bin/env python3
"""
Interactive Basketball Coach Test Client
- Mô phỏng luồng camera thực (Auto Stream 1fps)
- Cho phép đổi tư thế on-the-fly để test logic AI nhận diện sự thay đổi (Angle Change Threshold)
"""

import tkinter as tk
from tkinter import ttk, scrolledtext
import socketio
import base64
import io
import threading
import time
from datetime import datetime

try:
    import pygame
    pygame.mixer.init()
    AUDIO_AVAILABLE = True
except ImportError:
    print("⚠️  pygame not installed. Audio playback disabled.")
    AUDIO_AVAILABLE = False

import math

def generate_pose(elbow_angle_deg, knee_angle_deg, shoulder_angle_deg):
    pose = [ {"x": 0.5, "y": 0.2, "z": 0.0, "visibility": 0.99} for _ in range(33) ]
    
    # --- THÂN TRỤC CHÍNH ---
    pose[11] = {"x": 0.4, "y": 0.35, "z": 0.0, "visibility": 0.99} # Vai trái
    pose[23] = {"x": 0.4, "y": 0.65, "z": 0.0, "visibility": 0.99} # Hông trái
    
    # --- TAY (Xử lý góc Vai và Khuỷu Tay) ---
    arm_len = 0.2
    rad_shoulder = math.radians(shoulder_angle_deg)
    
    # Tính toạ độ Khuỷu tay dựa trên góc Vai (90 độ là giang ngang)
    pose[13] = {
        "x": pose[11]["x"] + arm_len * math.sin(rad_shoulder),
        "y": pose[11]["y"] + arm_len * math.cos(rad_shoulder),
        "z": 0.0, "visibility": 0.99
    }
    
    # Tính toạ độ Cổ tay dựa trên góc Khuỷu tay
    rad_elbow = math.radians(elbow_angle_deg)
    pose[15] = {
        "x": pose[13]["x"] + arm_len * math.sin(rad_shoulder - rad_elbow),
        "y": pose[13]["y"] + arm_len * math.cos(rad_shoulder - rad_elbow),
        "z": 0.0, "visibility": 0.99
    }
    
    # --- CHÂN (Xử lý góc Gối) ---
    pose[25] = {"x": 0.4, "y": 0.85, "z": 0.0, "visibility": 0.99} # Đầu gối (thẳng xuống từ hông)
    rad_knee = math.radians(180 - knee_angle_deg)
    leg_len = 0.2
    pose[27] = {
        "x": pose[25]["x"] - leg_len * math.sin(rad_knee),
        "y": pose[25]["y"] + leg_len * math.cos(rad_knee),
        "z": 0.0, "visibility": 0.99
    }

    # Clone data sang nửa người bên phải cho chuẩn form MediaPipe
    pose[12], pose[14], pose[16] = pose[11], pose[13], pose[15]
    pose[24], pose[26], pose[28] = pose[23], pose[25], pose[27]

    return pose


def get_current_pose(pose_type):
    if pose_type == "perfect":
        return generate_pose(90, 120, 90)
    elif pose_type == "low_elbow":
        return generate_pose(60, 120, 90) # Elbow < 80 (Too Low)
    elif pose_type == "straight_legs":
        return generate_pose(90, 170, 90) # Knee > 140 (Too High/Straight)
    return generate_pose(90, 120, 90)

class BasketballCoachClient:
    def __init__(self, root):
        self.root = root
        self.root.title("🏀 Test Client (Simulate Real-time Camera)")
        self.root.geometry("850x650")
        
        self.sio = socketio.Client()
        self.connected = False
        self.is_streaming = False
        self.frame_count = 0
        
        self.setup_ui()
        self.setup_socket_events()
        
    def setup_ui(self):
        conn_frame = ttk.Frame(self.root, padding="10")
        conn_frame.pack(fill=tk.X)
        
        ttk.Label(conn_frame, text="Server URL:").pack(side=tk.LEFT, padx=5)
        self.url_entry = ttk.Entry(conn_frame, width=40)
        self.url_entry.insert(0, "http://localhost:3000")
        self.url_entry.pack(side=tk.LEFT, padx=5)
        
        self.connect_btn = ttk.Button(conn_frame, text="Connect", command=self.toggle_connection)
        self.connect_btn.pack(side=tk.LEFT, padx=5)
        
        self.status_label = ttk.Label(conn_frame, text="⚫ Disconnected", foreground="red")
        self.status_label.pack(side=tk.LEFT, padx=10)
        
        control_frame = ttk.LabelFrame(self.root, text="Real-time Pose Modifier (Change on the fly)", padding="10")
        control_frame.pack(fill=tk.X, padx=10, pady=5)
        
        self.pose_var = tk.StringVar(value="low_elbow") 
        poses = [
            ("🔴 Bad: Low Elbow (<80°)", "low_elbow"),
            ("🔴 Bad: Straight Legs (>140°)", "straight_legs"),
            ("🟢 Good: Perfect Form", "perfect")
        ]
        for text, val in poses:
            ttk.Radiobutton(control_frame, text=text, variable=self.pose_var, value=val).pack(side=tk.LEFT, padx=10)
        
        btn_frame = ttk.Frame(self.root, padding="10")
        btn_frame.pack(fill=tk.X)
        
        # ĐỔI TEXT Ở ĐÂY (1fps)
        self.stream_btn = ttk.Button(btn_frame, text="▶️ Start Auto Stream (1fps)", 
                                     command=self.toggle_stream, state=tk.DISABLED, width=25)
        self.stream_btn.pack(side=tk.LEFT, padx=5)
        
        self.next_btn = ttk.Button(btn_frame, text="Step 1 Frame", 
                                   command=self.send_single_frame, state=tk.DISABLED)
        self.next_btn.pack(side=tk.LEFT, padx=5)
        
        self.shot_btn = ttk.Button(btn_frame, text="🎯 Release Shot (Trigger LLM)", 
                                   command=self.release_shot, state=tk.DISABLED)
        self.shot_btn.pack(side=tk.LEFT, padx=5)
        
        self.reset_btn = ttk.Button(btn_frame, text="🔄 Reset Session", 
                                    command=self.reset_session, state=tk.DISABLED)
        self.reset_btn.pack(side=tk.LEFT, padx=5)
        
        self.progress_label = ttk.Label(btn_frame, text="Frames sent: 0")
        self.progress_label.pack(side=tk.LEFT, padx=20)
        
        log_frame = ttk.LabelFrame(self.root, text="Server Responses", padding="10")
        log_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        self.log_text = scrolledtext.ScrolledText(log_frame, height=15, state=tk.DISABLED, bg="#f4f4f4")
        self.log_text.pack(fill=tk.BOTH, expand=True)
        
    def log(self, message, color="black"):
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.config(state=tk.NORMAL)
        self.log_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_text.see(tk.END)
        self.log_text.config(state=tk.DISABLED)
        
    def setup_socket_events(self):
        @self.sio.on('connect')
        def on_connect():
            self.connected = True
            self.root.after(0, self.on_connected)

        @self.sio.on('disconnect')
        def on_disconnect():
            self.connected = False
            self.root.after(0, self.on_disconnected)

        @self.sio.on('coach_feedback')
        def on_feedback(data):
            self.root.after(0, lambda: self.handle_feedback(data))

        @self.sio.on('angles_update')
        def on_angles(data):
            self.root.after(0, lambda: self.handle_angles(data))

        @self.sio.on('llm_post_shot_feedback')
        def on_llm_feedback(data):
            self.root.after(0, lambda: self.handle_llm_feedback(data))

    def toggle_connection(self):
        if self.connected:
            self.sio.disconnect()
        else:
            try:
                self.log("Connecting...")
                self.sio.connect(self.url_entry.get(), transports=['websocket'])
            except Exception as e:
                self.log(f"❌ Connection failed: {e}")
            
    def on_connected(self):
        self.status_label.config(text="🟢 Connected", foreground="green")
        self.connect_btn.config(text="Disconnect")
        self.stream_btn.config(state=tk.NORMAL)
        self.next_btn.config(state=tk.NORMAL)
        self.shot_btn.config(state=tk.NORMAL)
        self.reset_btn.config(state=tk.NORMAL)
        self.log("🟢 Connected successfully!")
        
    def on_disconnected(self):
        self.is_streaming = False
        self.status_label.config(text="⚫ Disconnected", foreground="red")
        self.connect_btn.config(text="Connect")
        self.stream_btn.config(text="▶️ Start Auto Stream (1fps)", state=tk.DISABLED)
        self.next_btn.config(state=tk.DISABLED)
        self.shot_btn.config(state=tk.DISABLED)
        self.reset_btn.config(state=tk.DISABLED)
        self.log("⚫ Disconnected.")

    def toggle_stream(self):
        if self.is_streaming:
            self.is_streaming = False
            self.stream_btn.config(text="▶️ Start Auto Stream (1fps)")
            self.log("⏸️ Stream paused.")
        else:
            self.is_streaming = True
            self.stream_btn.config(text="⏸️ Stop Auto Stream")
            self.log("▶️ Stream started. Sending frames every 1000ms...")
            self.send_stream_frame()

    def send_stream_frame(self):
        if not self.is_streaming or not self.connected:
            return
            
        self.send_single_frame(silent=False) # Để False để bạn thấy log bắn lên mỗi giây
        
        # ĐỔI THỜI GIAN LẶP Ở ĐÂY (1000ms = 1 giây)
        self.root.after(1000, self.send_stream_frame)

    def send_single_frame(self, silent=False):
        self.frame_count += 1
        pose_type = self.pose_var.get()
        pose_data = get_current_pose(pose_type)
        
        data = {
            "landmarks": pose_data,
            "timestamp": int(time.time() * 1000),
            "frameId": f"frame_{self.frame_count}",
            "exerciseType": "shooting"
        }
        
        self.sio.emit('pose_data', data)
        self.progress_label.config(text=f"Frames sent: {self.frame_count}")
        
        if not silent:
            self.log(f"📤 Sent frame {self.frame_count}: {pose_type}")

    def release_shot(self):
        self.is_streaming = False
        self.stream_btn.config(text="▶️ Start Auto Stream (1fps)")
        self.log("\n🎯 --- SHOT RELEASED --- Waiting for LLM...")
        self.sio.emit('shot_released')
        
    def reset_session(self):
        self.frame_count = 0
        self.progress_label.config(text="Frames sent: 0")
        self.sio.emit('reset_session')
        self.log("\n🔄 Session reset.")

    def handle_feedback(self, data):
        text = data.get('text', '')
        angles = data.get('angles', {})
        self.log(f"\n🗣️ COACH VOICE FEEDBACK: {text}")
        
        angle_str = []
        if 'elbowAngle' in angles: angle_str.append(f"Elbow: {angles['elbowAngle']:.1f}°")
        if 'kneeAngle' in angles: angle_str.append(f"Knee: {angles['kneeAngle']:.1f}°")
        if 'shoulderAngle' in angles: angle_str.append(f"Shoulder: {angles['shoulderAngle']:.1f}°") # Dòng mới thêm
        
        if angle_str:
            self.log(f"   [Triggered at -> {' | '.join(angle_str)}]")
            
        if 'audioBase64' in data:
            self.play_audio(data['audioBase64'])

    def handle_angles(self, data):
        pass

    def handle_llm_feedback(self, data):
        self.log("\n" + "="*50)
        self.log("🤖 LLM POST-SHOT ANALYSIS:")
        self.log(f"   {data.get('text', '')}")
        self.log("="*50 + "\n")
        if 'audioBase64' in data:
            self.play_audio(data['audioBase64'])

    def play_audio(self, audio_base64):
        if not AUDIO_AVAILABLE: return
        try:
            if audio_base64.startswith('data:'):
                audio_base64 = audio_base64.split(',')[1]
            audio_data = base64.b64decode(audio_base64)
            
            def play():
                try:
                    audio_io = io.BytesIO(audio_data)
                    pygame.mixer.music.load(audio_io)
                    pygame.mixer.music.play()
                except Exception as e:
                    pass
            threading.Thread(target=play, daemon=True).start()
        except:
            pass

if __name__ == "__main__":
    root = tk.Tk()
    app = BasketballCoachClient(root)
    root.mainloop()