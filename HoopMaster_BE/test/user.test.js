// Mock toàn bộ thư viện và Model trước khi gọi Service
jest.mock('bcryptjs');
jest.mock('../src/models/userModel'); // Nhớ check lại đường dẫn này trong project của bạn nhé

const bcrypt = require('bcryptjs');
const User = require('../src/models/userModel');
const userService = require('../src/services/userService');

describe('Kiểm thử Đơn vị: Quản lý Người dùng (userService)', () => {
  
  // Dọn dẹp trí nhớ của các hàm Mock sau mỗi bài test
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('1. Tính năng Đăng ký (Signup)', () => {
    
    it('USR_01: Đăng ký thành công, tự động mã hóa mật khẩu', async () => {
      // Giả lập: DB báo chưa có ai dùng email/username này (trả về null)
      User.findOne.mockResolvedValue(null); 
      // Giả lập: bcrypt băm mật khẩu thành công
      bcrypt.hash.mockResolvedValue('hashed_super_secret_password');
      
      // Giả lập: Hàm save() của Mongoose
      const mockSave = jest.fn().mockResolvedValue(true);
      User.mockImplementation(() => ({ save: mockSave }));

      const userData = { 
        username: 'curry30', 
        email: 'curry@nba.com', 
        password: 'password123', 
        name: 'Stephen Curry' 
      };
      
      await userService.signup(userData);

      // Kiểm tra xem hệ thống có check trùng lặp không
      expect(User.findOne).toHaveBeenCalled();
      // Kiểm tra xem mật khẩu có bị lộ thành plain-text không, hay đã được đem đi băm (hash)
      expect(bcrypt.hash).toHaveBeenCalledWith('password123', 10);
      // Kiểm tra dữ liệu có được lệnh lưu vào DB không
      expect(mockSave).toHaveBeenCalled();
    });

    it('USR_02: Chặn đứng nếu Username hoặc Email đã tồn tại', async () => {
      // Giả lập: DB tìm thấy một user đang dùng thông tin này
      User.findOne.mockResolvedValue({ username: 'curry30' });
      
      // Bắt buộc hàm signup phải ném ra lỗi
      await expect(
        userService.signup({ username: 'curry30', email: 'test@mail.com', password: '123' })
      ).rejects.toThrow('Username or email already exists');
    });
  });

  describe('2. Tính năng Đăng nhập (Login)', () => {
    
    it('USR_03: Đăng nhập thành công với mật khẩu chuẩn xác', async () => {
      const mockDbUser = { username: 'curry30', password: 'hashed_password_in_db' };
      
      // Giả lập: Tìm thấy user trong DB
      User.findOne.mockResolvedValue(mockDbUser);
      // Giả lập: Khớp mật khẩu
      bcrypt.compare.mockResolvedValue(true); 

      const result = await userService.login({ usernameOrEmail: 'curry30', password: 'correct_password' });
      
      // Trả về đúng object user
      expect(result.username).toBe('curry30');
      expect(bcrypt.compare).toHaveBeenCalledWith('correct_password', 'hashed_password_in_db');
    });

    it('USR_04: Báo lỗi "User not found" nếu nhập sai tài khoản', async () => {
      // Giả lập: DB không tìm thấy ai
      User.findOne.mockResolvedValue(null);

      await expect(
        userService.login({ usernameOrEmail: 'ghost_user', password: '123' })
      ).rejects.toThrow('User not found');
    });

    it('USR_05: Báo lỗi "Invalid credentials" nếu sai mật khẩu', async () => {
      // Giả lập: Tìm thấy tài khoản, nhưng bcrypt báo mật khẩu sai (false)
      User.findOne.mockResolvedValue({ username: 'curry30', password: 'hashed_password' });
      bcrypt.compare.mockResolvedValue(false);

      await expect(
        userService.login({ usernameOrEmail: 'curry30', password: 'wrong_password' })
      ).rejects.toThrow('Invalid credentials');
    });
  });

  describe('3. Tính năng Cập nhật Hồ sơ (Update Profile)', () => {
    
    it('USR_06: Cập nhật thành công Tone giọng HLV AI', async () => {
      const updatedUser = { _id: '123', tone: 'cheerful' };
      // Giả lập: Mongoose tìm và update thành công
      User.findByIdAndUpdate.mockResolvedValue(updatedUser);

      const result = await userService.updateProfile('123', { tone: 'cheerful' });
      
      expect(result.tone).toBe('cheerful');
      expect(User.findByIdAndUpdate).toHaveBeenCalledWith('123', { tone: 'cheerful' }, { new: true });
    });
  });
});