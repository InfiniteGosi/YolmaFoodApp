import ApiService from "../../services/ApiService";
import { Link, useNavigate } from "react-router-dom";
import { useConfirmDialog } from "./ConfirmDialog";

const Navbar = () => {
  const isAuthenticated = ApiService.isAuthenticated();
  const isAdmin = ApiService.isAdmin();
  const isCustomer = ApiService.isCustomer();
  const isDeliveryPerson = ApiService.isDeliveryPerson();
  const navigate = useNavigate();
  const { ConfirmDialog, showConfirm } = useConfirmDialog();

  const handleLogout = () => {
    showConfirm("Logout", "Are you sure you want to logout?", () => {
      ApiService.logout();
      navigate("/login");
    });
  };

  return (
    <nav>
      <div className="logo">
        <Link to="/" className="logo-link">
          Yolma
        </Link>
      </div>

      <div className="desktop-nav">
        <Link to="/home" className="nav-link">
          Home
        </Link>
        <Link to="/menu" className="nav-link">
          Menu
        </Link>
        <Link to="/categories" className="nav-link">
          Categories
        </Link>

        {isAuthenticated ? (
          <>
            {isCustomer &&
              ((
                <Link to="/orders" className="nav-link">
                  Orders
                </Link>
              ),
              (
                <Link to="/cart" className="nav-link">
                  Cart
                </Link>
              ))}
            {isDeliveryPerson && (
              <Link to="/deliveries" className="nav-link">
                Deliveries
              </Link>
            )}
            {isAdmin && (
              <Link to="/admin" className="nav-link">
                Admin
              </Link>
            )}
            <Link to="/profile" className="nav-link">
              Profile
            </Link>
            <button className="nav-button" onClick={handleLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="nav-link">
              Login
            </Link>
            <Link to="/register" className="nav-link">
              Register
            </Link>
          </>
        )}
        <ConfirmDialog />
      </div>
    </nav>
  );
};

export default Navbar;
