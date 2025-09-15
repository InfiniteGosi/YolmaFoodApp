import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import Navbar from "./components/common/NavBar";
import Footer from "./components/common/Footer";
import RegisterPage from "./components/auth/RegisterPage";
import AdminUserRegistration from "./components/admin/AdminUserRegistration";
import LoginPage from "./components/auth/LoginPage";
import HomePage from "./components/home/HomePage";
import CategoryPage from "./components/home/CategoryPage";
import MenuPage from "./components/home/MenuPage";
import MenuDetailsPage from "./components/home/MenuDetailsPage";
import ProfilePage from "./components/profileCart/ProfilePage";
import UpdateProfilePage from "./components/profileCart/UpdateProfilePage";
import OrderHistoryPage from "./components/profileCart/OrderHistoryPage";
import LeaveReviewPage from "./components/profileCart/LeaveReviewPage";
import CartPage from "./components/profileCart/CartPage";
import ProcessPaymentPage from "./components/payment/ProcessPaymentPage";
import { AdminRoute, CustomerRoute } from "./services/Guard";
import AdminLayout from "./components/admin/navbar/AdminLayout";
import AdminCategoriesPage from "./components/admin/AdminCategoriesPage";
import AdminCategoryFormPage from "./components/admin/AdminCategoryFormPage";
import AdminMenuPage from "./components/admin/AdminMenuPage";
import AdminMenuFormPage from "./components/admin/AdminMenuFormPage";
import AdminOrderPage from "./components/admin/AdminOrderPage";
import AdminOrderDetailsPage from "./components/admin/AdminOrderDetailsPage";
import AdminPaymentPage from "./components/admin/AdminPaymentPage";
import AdminPaymentDetailsPage from "./components/admin/AdminPaymentDetailsPage";
import AdminDashboardPage from "./components/admin/AdminDashboardPage";
import AdminUsersPage from "./components/admin/AdminUsersPage";
import AdminUpdateUserPage from "./components/admin/AdminUpdateUserPage";
import DeliveryOrderPage from "./components/delivery/DeliveryOrderPage";
import DeliveryOrderDetailsPage from "./components/delivery/DeliveryOrderDetailsPage";

function App() {
  return (
    <BrowserRouter>
      <div className="App">
        <Navbar />
        <div className="content">
          <Routes>
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/home" element={<HomePage />} />
            <Route path="/categories" element={<CategoryPage />} />
            <Route path="/menu" element={<MenuPage />} />
            <Route path="/menu/:id" element={<MenuDetailsPage />} />

            <Route
              path="/profile"
              element={<CustomerRoute element={<ProfilePage />} />}
            />

            <Route
              path="/update"
              element={<CustomerRoute element={<UpdateProfilePage />} />}
            />
            <Route
              path="/my-order-history"
              element={<CustomerRoute element={<OrderHistoryPage />} />}
            />

            <Route
              path="/leave-review"
              element={<CustomerRoute element={<LeaveReviewPage />} />}
            />

            <Route
              path="/cart"
              element={<CustomerRoute element={<CartPage />} />}
            />

            <Route
              path="/pay"
              element={<CustomerRoute element={<ProcessPaymentPage />} />}
            />

            <Route
              path="/deliveries"
              element={<CustomerRoute element={<DeliveryOrderPage />} />}
            />

            <Route
              path="/deliveries/orders/:id"
              element={<CustomerRoute element={<DeliveryOrderDetailsPage />} />}
            />

            <Route
              path="/admin"
              element={<AdminRoute element={<AdminLayout />} />}
            >
              <Route path="categories" element={<AdminCategoriesPage />} />
              <Route
                path="categories/new"
                element={<AdminCategoryFormPage />}
              />

              <Route path="menu-items" element={<AdminMenuPage />} />
              <Route path="menu-items/new" element={<AdminMenuFormPage />} />
              <Route
                path="menu-items/edit/:id"
                element={<AdminMenuFormPage />}
              />
              <Route
                path="categories/edit/:id"
                element={<AdminCategoryFormPage />}
              />

              <Route path="orders" element={<AdminOrderPage />} />
              <Route path="orders/:id" element={<AdminOrderDetailsPage />} />

              <Route path="payments" element={<AdminPaymentPage />} />
              <Route
                path="payments/:id"
                element={<AdminPaymentDetailsPage />}
              />

              <Route index element={<AdminDashboardPage />} />

              <Route path="users" element={<AdminUsersPage />} />
              <Route
                path="users/register"
                element={<AdminUserRegistration />}
              />
              <Route
                path="users/update/:id"
                element={<AdminUpdateUserPage />}
              />
            </Route>

            <Route path="*" element={<Navigate to={"/home"} />} />
          </Routes>
        </div>
      </div>

      <Footer />
    </BrowserRouter>
  );
}

export default App;
