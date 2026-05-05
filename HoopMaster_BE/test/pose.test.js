const poseService = require('../src/services/poseService');

describe('Kiểm thử Đơn vị: Bộ não Toán học 3D (poseService)', () => {

  describe('1. Hàm calculateAngle3D (Tính góc 3D bằng Vector)', () => {
    
    it('POS_01: Tính chính xác góc vuông (90 độ)', () => {
      const p1 = { x: 1, y: 0, z: 0 }; 
      const p2 = { x: 0, y: 0, z: 0 }; 
      const p3 = { x: 0, y: 1, z: 0 }; 

      const angle = poseService.calculateAngle3D(p1, p2, p3);
      expect(angle).toBeCloseTo(90, 1); 
    });

    it('POS_02: Tính chính xác góc bẹt / Đường thẳng (180 độ)', () => {
      const p1 = { x: 1, y: 0, z: 0 }; 
      const p2 = { x: 0, y: 0, z: 0 }; 
      const p3 = { x: -1, y: 0, z: 0 }; 

      const angle = poseService.calculateAngle3D(p1, p2, p3);
      expect(angle).toBeCloseTo(180, 1);
    });

    it('POS_03: Xử lý an toàn khi bị thiếu điểm (Trả về null thay vì crash)', () => {
      const angle = poseService.calculateAngle3D(null, { x: 0, y: 0, z: 0 }, { x: 1, y: 1, z: 1 });
      
      // Code của bạn thiết kế trả về null rất chuẩn xác!
      expect(angle).toBeNull(); 
    });
  });

  describe('2. Hàm calculateDistance (Khoảng cách không gian)', () => {
    
    it('POS_04: Tính đúng định lý Pythagoras (Tam giác 3-4-5)', () => {
      const p1 = { x: 0, y: 0, z: 0 };
      const p2 = { x: 0, y: 3, z: 4 }; 

      const dist = poseService.calculateDistance(p1, p2);
      expect(dist).toBe(5);
    });
    
  });

  describe('3. Hàm isValidPoint (Kiểm tra độ tin cậy)', () => {
    
    it('POS_05: Nhận diện điểm TỐT, XẤU và điểm không có visibility', () => {
      const goodPoint = { x: 1, y: 1, z: 1, visibility: 0.9 }; 
      const badPoint = { x: 1, y: 1, z: 1, visibility: 0.1 };  
      const missingVisPoint = { x: 1, y: 1, z: 1 };            

      // Điểm rõ ràng -> Hợp lệ
      expect(poseService.isValidPoint(goodPoint)).toBe(true);
      
      // Điểm mờ, rớt tracking -> Loại bỏ
      expect(poseService.isValidPoint(badPoint)).toBe(false);

      // Điểm mất field visibility nhưng vẫn có tọa độ -> Chấp nhận (Fallback)
      expect(poseService.isValidPoint(missingVisPoint)).toBe(true);
    });

  });
});