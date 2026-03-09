import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import { getTariffInfo, purchaseTariff } from "../api";

export default function TariffsPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [currentTariff, setCurrentTariff] = useState(null);
  const [loading, setLoading] = useState(false);
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const tariffs = [
    {
      id: 'BASIC',
      name: 'Базовый',
      price: '199 ₽/мес',
      storage: '10 GB',
      features: ['10 GB хранилища', 'До 5 пользователей', 'Поддержка 24/7']
    },
    {
      id: 'WORK',
      name: 'Рабочий',
      price: '499 ₽/мес',
      storage: '50 GB',
      features: ['50 GB хранилища', 'До 20 пользователей', 'Приоритетная поддержка']
    },
    {
      id: 'PREMIUM',
      name: 'Премиум',
      price: '999 ₽/мес',
      storage: '100 GB',
      features: ['100 GB хранилища', 'Неограничено пользователей', 'VIP поддержка']
    }
  ];

  useEffect(() => {
    loadCurrentTariff();
  }, []);

  const loadCurrentTariff = async () => {
    setLoading(true);
    try {
      const info = await getTariffInfo(token);
      console.log("Tariff info from server:", info);
      setCurrentTariff(info);
    } catch (err) {
      console.error('Failed to load tariff info:', err);
      setError('Не удалось загрузить информацию о тарифе');
    } finally {
      setLoading(false);
    }
  };

  const handlePurchase = async (tariffId) => {
    setPurchasing(true);
    setError('');
    setSuccess('');

    try {
      const selectedTariff = tariffs.find(t => t.id === tariffId);
      const confirm = window.confirm(`Купить тариф "${selectedTariff.name}" за ${selectedTariff.price}?`);

      if (confirm) {
        await purchaseTariff(token, tariffId, 'test-payment-token');
        await loadCurrentTariff();
        setSuccess(`Тариф "${selectedTariff.name}" успешно приобретен!`);
      }
    } catch (err) {
      setError('Ошибка при покупке тарифа');
      console.error(err);
    } finally {
      setPurchasing(false);
    }
  };

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatEndDate = (dateString) => {
    // Если даты нет - считаем что это TRIAL (30 дней от сегодня)
    if (!dateString) {
      const trialEndDate = new Date();
      trialEndDate.setDate(trialEndDate.getDate() + 30);
      return trialEndDate.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    }

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        // Невалидная дата - тоже 30 дней
        const trialEndDate = new Date();
        trialEndDate.setDate(trialEndDate.getDate() + 30);
        return trialEndDate.toLocaleDateString('ru-RU', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric'
        });
      }
      return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      // Ошибка парсинга - 30 дней
      const trialEndDate = new Date();
      trialEndDate.setDate(trialEndDate.getDate() + 30);
      return trialEndDate.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    }
  };

  const getProgressBarColor = (percentage) => {
    if (percentage < 50) return 'bg-green-500';
    if (percentage < 75) return 'bg-yellow-500';
    if (percentage < 90) return 'bg-orange-500';
    return 'bg-red-500';
  };

  return (
      <div className="h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex flex-col overflow-hidden">
        {/* Верхняя панель с кнопкой назад слева */}
        <div className="flex justify-between items-center mb-6 flex-shrink-0">
          <button
              onClick={() => navigate(-1)}
              className="flex items-center text-white/70 hover:text-white transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Назад
          </button>

          <div className="text-center">
            <h1 className="text-3xl md:text-4xl font-bold text-white">
              Тарифные планы
            </h1>
            <p className="text-white/60 text-sm mt-1">Выберите подходящий тариф</p>
          </div>

          <div className="relative w-32 flex justify-end">
            <button
                className="flex items-center space-x-3 bg-white/10 hover:bg-white/15 backdrop-blur-sm rounded-xl px-4 py-2.5 transition-all duration-200 border border-white/10 hover:border-white/20"
            >
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500/90 to-purple-500/90 rounded-full flex items-center justify-center text-white font-bold shadow-md">
                {((user?.name || user?.username || user?.email || "U").charAt(0)).toUpperCase()}
              </div>
              <div className="text-left">
                <span className="font-medium text-white text-sm block">
                  {user?.name || user?.username || user?.email?.split('@')[0] || "Пользователь"}
                </span>
                {user?.email && <span className="text-white/50 text-xs block">{user.email}</span>}
              </div>
            </button>
          </div>
        </div>

        {/* Основной контент - без скролла, всё влезает */}
        <div className="flex-1 flex items-center justify-center">
          <div className="w-full max-w-6xl">
            {/* Текущий тариф */}
            {currentTariff && (
                <div className="mb-8 bg-gradient-to-r from-blue-600/20 to-purple-600/20 backdrop-blur-sm rounded-2xl p-6 border border-white/10">
                  <h2 className="text-xl font-semibold mb-4">Текущий тариф</h2>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="bg-white/10 rounded-xl p-4">
                      <div className="text-sm text-white/60 mb-1">Тариф</div>
                      <div className="text-2xl font-bold text-yellow-300">{currentTariff.tariffPlan || 'TRIAL'}</div>
                    </div>
                    <div className="bg-white/10 rounded-xl p-4">
                      <div className="text-sm text-white/60 mb-1">Использовано</div>
                      <div className="text-2xl font-bold text-blue-300">{formatBytes(currentTariff.usedStorage || 0)}</div>
                    </div>
                    <div className="bg-white/10 rounded-xl p-4">
                      <div className="text-sm text-white/60 mb-1">Лимит</div>
                      <div className="text-2xl font-bold text-green-300">{formatBytes(currentTariff.storageLimit || 10 * 1024 * 1024 * 1024)}</div>
                    </div>
                    <div className="bg-white/10 rounded-xl p-4">
                      <div className="text-sm text-white/60 mb-1">Действует до</div>
                      <div className="text-lg font-bold text-white">
                        {formatEndDate(currentTariff.endDate)}
                      </div>
                    </div>
                  </div>

                  {/* Прогресс-бар */}
                  <div className="mt-4">
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-white/80">Заполнено</span>
                      <span className="text-blue-300">
                        {Math.round(((currentTariff.usedStorage || 0) / (currentTariff.storageLimit || 10 * 1024 * 1024 * 1024)) * 100)}%
                      </span>
                    </div>
                    <div className="w-full bg-gray-700/50 rounded-full h-2 overflow-hidden">
                      <div
                          className={`h-2 rounded-full transition-all duration-300 ${getProgressBarColor(Math.round(((currentTariff.usedStorage || 0) / (currentTariff.storageLimit || 10 * 1024 * 1024 * 1024)) * 100))}`}
                          style={{ width: `${Math.min(Math.round(((currentTariff.usedStorage || 0) / (currentTariff.storageLimit || 10 * 1024 * 1024 * 1024)) * 100), 100)}%` }}
                      />
                    </div>
                  </div>
                </div>
            )}

            {/* Сообщения об ошибках/успехе */}
            {error && (
                <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center">
                  {error}
                </div>
            )}

            {success && (
                <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl text-center">
                  {success}
                </div>
            )}

            {/* Сетка тарифов */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {tariffs.map((tariff) => (
                  <div
                      key={tariff.id}
                      className={`bg-white/10 backdrop-blur-xl rounded-2xl p-6 border-2 transition-all duration-300 hover:scale-105 ${
                          currentTariff?.tariffPlan === tariff.id
                              ? 'border-yellow-400 shadow-lg shadow-yellow-500/20'
                              : 'border-white/10 hover:border-blue-500/50'
                      }`}
                  >
                    <h2 className="text-2xl font-bold mb-2">{tariff.name}</h2>
                    <div className="text-3xl font-bold text-yellow-300 mb-4">{tariff.price}</div>
                    <div className="text-xl text-blue-300 mb-6">{tariff.storage}</div>

                    <ul className="space-y-3 mb-8">
                      {tariff.features.map((feature, index) => (
                          <li key={index} className="flex items-center text-white/80">
                            <svg className="w-5 h-5 mr-2 text-green-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                            <span>{feature}</span>
                          </li>
                      ))}
                    </ul>

                    <button
                        onClick={() => handlePurchase(tariff.id)}
                        disabled={purchasing || currentTariff?.tariffPlan === tariff.id}
                        className={`w-full py-3 rounded-xl font-medium transition-all ${
                            currentTariff?.tariffPlan === tariff.id
                                ? 'bg-green-600/50 cursor-not-allowed'
                                : 'bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-500 hover:to-cyan-500'
                        }`}
                    >
                      {purchasing ? (
                          <div className="flex items-center justify-center">
                            <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white mr-2"></div>
                            <span>Обработка...</span>
                          </div>
                      ) : currentTariff?.tariffPlan === tariff.id ? (
                          'Текущий тариф'
                      ) : (
                          'Выбрать'
                      )}
                    </button>
                  </div>
              ))}
            </div>
          </div>
        </div>
      </div>
  );
}