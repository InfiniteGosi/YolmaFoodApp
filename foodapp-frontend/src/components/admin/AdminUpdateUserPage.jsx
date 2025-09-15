import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";
import { useConfirmDialog } from "../common/ConfirmDialog";

const AdminUpdateUserPage = () => {
  const { id } = useParams();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [address, setAddress] = useState("");
  const [isActive, setIsActive] = useState(true);

  const [profileImage, setProfileImage] = useState(null);
  const [previewImage, setPreviewImage] = useState("");

  const fileInputRef = useRef(null);

  const navigate = useNavigate();
  const { ErrorDisplay, showError } = useError();
  const { ConfirmDialog, showConfirm } = useConfirmDialog();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await ApiService.getUserById(id);
        if (response.statusCode === 200) {
          const userData = response.data;
          setName(userData.name);
          setEmail(userData.email);
          setPhoneNumber(userData.phoneNumber);
          setAddress(userData.address);
          setPreviewImage(userData.profileUrl);
          setIsActive(userData.isActive);
        }
      } catch (error) {
        showError(error.response?.data?.message || error.message);
      }
    };
    fetchUser();
  }, [id]);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setProfileImage(file);
      setPreviewImage(URL.createObjectURL(file));
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current.click();
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    showConfirm(
      "Update User",
      "Are you sure you want to update this user?",
      async () => {
        try {
          const formData = new FormData();
          formData.append("name", name);
          formData.append("email", email);
          formData.append("phoneNumber", phoneNumber);
          formData.append("address", address);
          formData.append("isActive", isActive);

          if (profileImage) {
            formData.append("imageFile", profileImage);
          }

          const response = await ApiService.updateUser(id, formData);

          if (response.statusCode === 200) {
            navigate("/admin/users");
          }
        } catch (error) {
          showError(error.response?.data?.message || error.message);
        }
      }
    );
  };

  return (
    <div className="profile-container">
      <ErrorDisplay />

      <div className="profile-header">
        <h1 className="profile-title">Admin Update User</h1>
        <div className="profile-image-container">
          <div className="profile-avatar">
            {previewImage ? (
              <img
                src={previewImage}
                alt="Profile"
                className="profile-image-edit"
                onClick={triggerFileInput}
              />
            ) : (
              <div className="avatar-fallback">
                {name.substring(0, 2).toUpperCase()}
              </div>
            )}
          </div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageChange}
            accept="image/*"
            style={{ display: "none" }}
          />
          <button className="profile-image-upload" onClick={triggerFileInput}>
            Change Photo
          </button>
        </div>
      </div>

      <form className="profile-form" onSubmit={handleUpdateUser}>
        <div className="form-grid">
          <div className="profile-form-group">
            <label htmlFor="name" className="profile-form-label">
              Name:
            </label>
            <input
              type="text"
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="profile-form-input"
              required
            />
          </div>
          <div className="profile-form-group">
            <label htmlFor="email" className="profile-form-label">
              Email:
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="profile-form-input"
              required
            />
          </div>
          <div className="profile-form-group">
            <label htmlFor="phoneNumber" className="profile-form-label">
              Phone:
            </label>
            <input
              type="tel"
              id="phoneNumber"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              className="profile-form-input"
              required
            />
          </div>
          <div className="profile-form-group">
            <label htmlFor="address" className="profile-form-label">
              Address:
            </label>
            <input
              type="text"
              id="address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="profile-form-input"
              required
            />
          </div>

          <div className="profile-form-group">
            <label htmlFor="isActive" className="profile-form-label">
              Active:
            </label>
            <label className="toggle-switch">
              <input
                type="checkbox"
                id="isActive"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
              />
              <span className="slider"></span>
            </label>
          </div>
        </div>

        <button type="submit" className="admin-register-button">
          Update User
        </button>
        <button
          className="admin-deregister-button"
          onClick={() => navigate("/admin/users")}
        >
          Back to users
        </button>
      </form>

      <ConfirmDialog />
    </div>
  );
};

export default AdminUpdateUserPage;
