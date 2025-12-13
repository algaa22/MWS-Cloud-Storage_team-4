import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import { updateUserInfo, getUserInfo } from "../api.js";

export default function SettingsPage() {
  const { user, token, updateUser } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [userDetails, setUserDetails] = useState(null);

  const [usernameForm, setUsernameForm] = useState({
    newUsername: "",
    show: false
  });

  const [passwordForm, setPasswordForm] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
    show: false
  });

  const [passwordError, setPasswordError] = useState("");

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
    loadUserDetails();
  }, [token, navigate]);

  const loadUserDetails = async () => {
    try {
      const data = await getUserInfo(token);
      setUserDetails(data);
    } catch (err) {
      console.error("Error loading user details:", err);
      setError("Не удалось загрузить информацию о пользователе");
    }
  };

  const handleUsernameUpdate = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError("");
      setSuccess("");

      const newUsernameValue = usernameForm.newUsername.trim();
      console.log("Will update username to:", newUsernameValue);

      if (!newUsernameValue) {
        setError("Пожалуйста, введите новое имя пользователя");
        setLoading(false);
        return;
      }

      const response = await updateUserInfo(token, {
        newUsername: newUsernameValue
      });

      console.log("Update response:", response);

      if (response.success !== false) {
        setSuccess("Имя пользователя успешно обновлено!");

        if (updateUser) {
          updateUser({ ...user, username: newUsernameValue });
        }

        setUsernameForm({ newUsername: "", show: false });
        await loadUserDetails();
      } else {
        setError(`Обновление не удалось: ${response.message || 'Неизвестная ошибка'}`);
      }

    } catch (error) {
      console.error("Update error:", error);
      setError(`Ошибка: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordUpdate = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError("");
      setSuccess("");
      setPasswordError(""); // Очищаем ошибку пароля

      if (!passwordForm.oldPassword || !passwordForm.newPassword) {
        setPasswordError("Пожалуйста, введите и старый, и новый пароль");
        setLoading(false);
        return;
      }

      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        setPasswordError("Новые пароли не совпадают");
        setLoading(false);
        return;
      }

      if (passwordForm.newPassword.length < 6) {
        setPasswordError("Пароль должен содержать минимум 6 символов");
        setLoading(false);
        return;
      }

      if (passwordForm.oldPassword === passwordForm.newPassword) {
        setPasswordError("Новый пароль должен отличаться от старого");
        setLoading(false);
        return;
      }

      console.log("Updating password...");

      const response = await updateUserInfo(token, {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      });

      console.log("Password update response:", response);

      if (response.success !== false) {
        setSuccess("Пароль успешно обновлен!");
        setPasswordError(""); // Очищаем ошибку при успехе

        setPasswordForm({
          oldPassword: "",
          newPassword: "",
          confirmPassword: "",
          show: false
        });
      } else {
        const errorMsg = response.message || 'Неизвестная ошибка';
        if (errorMsg.includes("Password incorrect") ||
            errorMsg.includes("Wrong password") ||
            errorMsg.includes("Неверный пароль")) {
          setPasswordError("Неверный текущий пароль");
        } else {
          setPasswordError(`Ошибка: ${errorMsg}`);
        }
      }

    } catch (error) {
      console.error("Password update error:", error);

      if (error.message.includes("Password incorrect") ||
          error.message.includes("Wrong password")) {
        setPasswordError("Неверный текущий пароль");
      } else if (error.message.includes("Old password is required")) {
        setPasswordError("Требуется ввести старый пароль");
      } else {
        setPasswordError(`Ошибка: ${error.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-4">
        <header className="flex justify-between items-center mb-8">
          <h1 className="text-2xl font-bold">Настройки профиля</h1>
          <button
              onClick={() => navigate("/files")}
              className="px-4 py-2 bg-white/20 backdrop-blur-sm rounded-xl hover:bg-white/30 transition-colors"
          >
            ← Назад к файлам
          </button>
        </header>

        <div className="max-w-4xl mx-auto">
          <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-6 mb-6">
            <div className="flex items-center space-x-4 mb-6">
              <div className="w-16 h-16 bg-blue-500 rounded-full flex items-center justify-center text-2xl font-bold">
                {user?.username?.[0]?.toUpperCase() || "U"}
              </div>
              <div>
                <h2 className="text-xl font-bold">{user?.username || "Пользователь"}</h2>
                <p className="text-white/70">{user?.email}</p>
              </div>
            </div>

            {/* Общие сообщения */}
            {error && (
                <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl">
                  {error}
                </div>
            )}

            {success && (
                <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl">
                  {success}
                </div>
            )}

            {/* Смена имени пользователя */}
            <div className="mb-8">
              <div className="flex justify-between items-center mb-4">
                <div>
                  <h3 className="text-lg font-medium">Имя пользователя</h3>
                  <p className="text-white/60">Текущее имя: {user?.username}</p>
                </div>
                <button
                    onClick={() => setUsernameForm({
                      ...usernameForm,
                      show: !usernameForm.show,
                      newUsername: user?.username || ""
                    })}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-xl transition-colors"
                >
                  {usernameForm.show ? "Отмена" : "Изменить"}
                </button>
              </div>

              {usernameForm.show && (
                  <form onSubmit={handleUsernameUpdate} className="space-y-4">
                    <div>
                      <label className="block text-sm mb-2">Новое имя пользователя</label>
                      <input
                          type="text"
                          value={usernameForm.newUsername}
                          onChange={(e) => setUsernameForm({ ...usernameForm, newUsername: e.target.value })}
                          className="w-full p-3 bg-white/20 rounded-xl text-white"
                          placeholder="Введите новое имя"
                          disabled={loading}
                      />
                    </div>
                    <button
                        type="submit"
                        disabled={loading || !usernameForm.newUsername.trim()}
                        className="px-6 py-3 bg-green-600 hover:bg-green-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {loading ? "Сохранение..." : "Сохранить имя"}
                    </button>
                  </form>
              )}
            </div>

            {/* Смена пароля */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <div>
                  <h3 className="text-lg font-medium">Пароль</h3>
                  <p className="text-white/60">Измените пароль вашей учетной записи</p>
                </div>
                <button
                    onClick={() => {
                      setPasswordForm({
                        ...passwordForm,
                        show: !passwordForm.show
                      });
                      setPasswordError(""); // Очищаем ошибку при открытии/закрытии
                    }}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-xl transition-colors"
                >
                  {passwordForm.show ? "Отмена" : "Изменить пароль"}
                </button>
              </div>

              {passwordForm.show && (
                  <form onSubmit={handlePasswordUpdate} className="space-y-4">
                    <div>
                      <label className="block text-sm mb-2">Текущий пароль</label>
                      <input
                          type="password"
                          value={passwordForm.oldPassword}
                          onChange={(e) => {
                            setPasswordForm({ ...passwordForm, oldPassword: e.target.value });
                            setPasswordError(""); // Очищаем ошибку при изменении
                          }}
                          className={`w-full p-3 rounded-xl text-white ${
                              passwordError.includes("Неверный текущий пароль")
                                  ? "bg-red-500/20 border-2 border-red-500"
                                  : "bg-white/20"
                          }`}
                          placeholder="Введите текущий пароль"
                          disabled={loading}
                      />
                      {passwordError.includes("Неверный текущий пароль") && (
                          <p className="mt-1 text-sm text-red-400">{passwordError}</p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm mb-2">Новый пароль</label>
                      <input
                          type="password"
                          value={passwordForm.newPassword}
                          onChange={(e) => {
                            setPasswordForm({ ...passwordForm, newPassword: e.target.value });
                            setPasswordError(""); // Очищаем ошибку при изменении
                          }}
                          className={`w-full p-3 rounded-xl text-white ${
                              passwordError &&
                              !passwordError.includes("Неверный текущий пароль") &&
                              passwordError !== "Новые пароли не совпадают" &&
                              !passwordError.includes("минимум 6 символов")
                                  ? "bg-red-500/20 border-2 border-red-500"
                                  : "bg-white/20"
                          }`}
                          placeholder="Введите новый пароль"
                          disabled={loading}
                      />
                      {passwordError &&
                          !passwordError.includes("Неверный текущий пароль") &&
                          passwordError !== "Новые пароли не совпадают" &&
                          !passwordError.includes("минимум 6 символов") && (
                              <p className="mt-1 text-sm text-red-400">{passwordError}</p>
                          )}
                    </div>

                    <div>
                      <label className="block text-sm mb-2">Подтвердите новый пароль</label>
                      <input
                          type="password"
                          value={passwordForm.confirmPassword}
                          onChange={(e) => {
                            setPasswordForm({ ...passwordForm, confirmPassword: e.target.value });
                            if (passwordError === "Новые пароли не совпадают") {
                              setPasswordError(""); // Очищаем ошибку при изменении
                            }
                          }}
                          className={`w-full p-3 rounded-xl text-white ${
                              passwordError === "Новые пароли не совпадают"
                                  ? "bg-red-500/20 border-2 border-red-500"
                                  : "bg-white/20"
                          }`}
                          placeholder="Повторите новый пароль"
                          disabled={loading}
                      />
                      {passwordError === "Новые пароли не совпадают" && (
                          <p className="mt-1 text-sm text-red-400">{passwordError}</p>
                      )}
                    </div>

                    {/* Общее сообщение об ошибке пароля, если не связано с конкретным полем */}
                    {passwordError &&
                        passwordError.includes("минимум 6 символов") && (
                            <div className="p-3 bg-red-500/20 border border-red-500 rounded-xl">
                              <p className="text-red-300">{passwordError}</p>
                            </div>
                        )}

                    <button
                        type="submit"
                        disabled={loading ||
                            !passwordForm.oldPassword ||
                            !passwordForm.newPassword ||
                            passwordForm.newPassword !== passwordForm.confirmPassword ||
                            passwordForm.newPassword.length < 6
                        }
                        className="px-6 py-3 bg-green-600 hover:bg-green-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {loading ? "Сохранение..." : "Изменить пароль"}
                    </button>
                  </form>
              )}
            </div>
          </div>

          {/* Информация о хранилище */}
          {userDetails?.storageInfo && (
              <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-6">
                <h3 className="text-lg font-medium mb-4">Информация о хранилище</h3>
                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between text-sm mb-1">
                      <span>Использовано:</span>
                      <span className="text-blue-300">{userDetails.storageInfo.formattedUsed}</span>
                    </div>
                    <div className="w-full bg-gray-700 rounded-full h-2">
                      <div
                          className="h-2 rounded-full bg-blue-500 transition-all duration-300"
                          style={{ width: `${Math.min(userDetails.storageInfo.percentage, 100)}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-xs text-white/60 mt-1">
                      <span>Лимит: {userDetails.storageInfo.formattedTotal}</span>
                      <span>{userDetails.storageInfo.percentage}%</span>
                    </div>
                  </div>
                </div>
              </div>
          )}
        </div>
      </div>
  );
}