import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";
import { useConfirmDialog } from "../common/ConfirmDialog";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlus, faEdit, faBan } from "@fortawesome/free-solid-svg-icons";

const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const { ErrorDisplay, showError } = useError();
  const { ConfirmDialog } = useConfirmDialog();
  const navigate = useNavigate();

  // Fetch all users
  const fetchUsers = async () => {
    try {
      const response = await ApiService.getAllUsers();
      if (response.statusCode === 200) {
        setUsers(response.data);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  // Fetch current admin
  const fetchMyProfile = async () => {
    try {
      const resp = await ApiService.myProfile();
      if (resp.statusCode === 200) {
        setCurrentUser(resp.data);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchMyProfile();
  }, []);

  const handleRegisterUser = () => {
    navigate("/admin/users/register");
  };

  const handleUpdateUser = (id) => {
    navigate(`/admin/users/update/${id}`);
  };

  return (
    <div className="admin-users">
      <ErrorDisplay />
      <div className="content-header">
        <h1>User Management</h1>
        <button className="add-btn" onClick={handleRegisterUser}>
          <FontAwesomeIcon icon={faPlus} /> Register User
        </button>
      </div>

      <div className="users-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Roles</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.roles?.map((r) => r.name).join(", ")}</td>
                <td>{user.isActive ? "Yes" : "No"}</td>
                <td className="actions">
                  {currentUser?.id === user.id ? (
                    <button className="dummy-btn" disabled>
                      <FontAwesomeIcon icon={faBan} /> It&apos;s you
                    </button>
                  ) : (
                    <button
                      className="edit-btn"
                      onClick={() => handleUpdateUser(user.id)}
                    >
                      <FontAwesomeIcon icon={faEdit} /> Update
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmDialog />
    </div>
  );
};

export default AdminUsersPage;
