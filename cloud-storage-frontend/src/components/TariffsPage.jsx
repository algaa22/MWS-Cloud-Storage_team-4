import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import { getTariffInfo, purchaseTariff, setAutoRenew, getUserInfo } from "../api";
import PaymentModal from "./PaymentModal";

export default function TariffsPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [currentTariff, setCurrentTariff] = useState(null);
  const [loading, setLoading] = useState(false);
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [selectedTariff, setSelectedTariff] = useState(null);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [usedStorage, setUsedStorage] = useState(0);

  const tariffs = [
    {
      id: 'BASIC',
      name: 'Базовый',
      price: '99 ₽/мес',
      storage: '10 GB',
      features: ['10 GB платного хранилища', '5 GB бесплатно всегда', 'До 3 пользователей', 'Поддержка 24/7']
    },
    {
      id: 'WORK',
      name: 'Рабочий',
      price: '199 ₽/мес',
      storage: '50 GB',
      features: ['50 GB платного хранилища', '5 GB бесплатно всегда', 'До 10 пользователей', 'Приоритетная поддержка']
    },
    {
      id: 'PREMIUM',
      name: 'Премиум',
      price: '349 ₽/мес',
      storage: '100 GB',
      features: ['100 GB платного хранилища', '5 GB бесплатно всегда', 'До 30 пользователей', 'VIP поддержка']
    }
  ];

  useEffect(() => {
    loadCurrentTariff();
    loadUsedStorage();
  }, []);

const loadCurrentTariff = async () => {
  setLoading(true);
  try {
    const info = await getTariffInfo(token);
    console.log("=== TARIFF INFO RECEIVED ===");
    console.log("Full object:", info);
    console.log("usedStorage:", info.usedStorage);
    console.log("totalStorageLimit:", info.totalStorageLimit);
    console.log("freeStorageLimit:", info.freeStorageLimit);
    console.log("activeTariff:", info.activeTariff);
    console.log("All keys:", Object.keys(info));
    setCurrentTariff(info);
  } catch (err) {
    console.error('Failed to load tariff info:', err);
    setError('Не удалось загрузить информацию о тарифе');
  } finally {
    setLoading(false);
  }
};

const loadUsedStorage = async () => {
  try {
    const userInfo = await getUserInfo(token);
    console.log("User info for storage:", userInfo);
    setUsedStorage(userInfo.usedStorage || 0);
  } catch (err) {
    console.error("Failed to load used storage:", err);
  }
};

  const handlePurchase = (tariffId) => {
    const tariff = tariffs.find(t => t.id === tariffId);
    setSelectedTariff(tariff);
    setIsPaymentModalOpen(true);
  };

  const handleConfirmPayment = async (paymentMethod, autoRenew) => {
    setPurchasing(true);
    setError('');
    setSuccess('');

    try {
      console.log(`Оплата тарифа "${selectedTariff.name}" методом: ${paymentMethod}, автопродление: ${autoRenew ? 'вкл' : 'выкл'}`);

      await purchaseTariff(token, selectedTariff.id, 'test-payment-token', autoRenew, paymentMethod);
      await loadCurrentTariff();
      await loadUsedStorage();

      sessionStorage.setItem('refreshStorage', 'true');

      setSuccess(`Тариф "${selectedTariff.name}" успешно приобретен!`);
      setIsPaymentModalOpen(false);
      setSelectedTariff(null);
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

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return '—';
      return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return '—';
    }
  };

  const getProgressBarColor = (percentage) => {
    if (percentage < 50) return 'bg-green-500';
    if (percentage < 75) return 'bg-yellow-500';
    if (percentage < 90) return 'bg-orange-500';
    return 'bg-red-500';
  };

  const calculatePercentage = () => {
    if (!currentTariff) return 0;

    const used = usedStorage; // ← ИСПОЛЬЗУЕМ usedStorage ИЗ СОСТОЯНИЯ
    const limit = currentTariff.totalStorageLimit || (5 * 1024 * 1024 * 1024);

    if (limit === 0) return 0;

    const percent = Math.min(Math.round((used / limit) * 100), 100);

    console.log(`📊 Storage calculation: used=${used}, limit=${limit}, percent=${percent}%`);

    return percent;
  };

  const handleToggleAutoRenew = async () => {
    if (!currentTariff || !currentTariff.activeTariff) return;

    const newState = !currentTariff.autoRenew;

    try {
      setPurchasing(true);
      await setAutoRenew(token, newState);
      await loadCurrentTariff();
      setSuccess(`Автопродление ${newState ? 'включено' : 'отключено'}`);
    } catch (err) {
      setError('Ошибка при изменении автопродления');
      console.error(err);
    } finally {
      setPurchasing(false);
    }
  };

  return (
    <div className="h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex flex-col overflow-hidden">
      {/* Верхняя панель */}
      <div className="flex justify-between items-center mb-4 flex-shrink-0">
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
          <button className="flex items-center space-x-3 bg-white/10 hover:bg-white/15 backdrop-blur-sm rounded-xl px-4 py-2.5 transition-all duration-200 border border-white/10 hover:border-white/20">
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

      {/* Основной контент */}
      <div className="flex-1 flex flex-col">
        {/* Блок текущего тарифа */}
        {currentTariff && (
          <div className="bg-gradient-to-r from-blue-600/20 to-purple-600/20 backdrop-blur-sm rounded-2xl p-5 border border-white/10 mb-6">
            {/* Статус пользователя */}
            {currentTariff.isActive === false && (
              <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl">
                <div className="flex items-center">
                  <svg className="w-5 h-5 text-red-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <span className="text-red-300 font-medium">Аккаунт ограничен. Оплатите тариф для восстановления доступа.</span>
                </div>
              </div>
            )}

            {/* Пробный период */}
            {currentTariff.hasActiveTrial && (
              <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl">
                <div className="flex items-center justify-between">
                  <div className="flex items-center">
                    <svg className="w-5 h-5 text-green-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span className="text-green-300 font-medium">Пробный период активен!</span>
                  </div>
                  <span className="text-green-300 text-sm">
                    Действует до: {formatDate(currentTariff.trialEndDate)}
                  </span>
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
              <div className="bg-white/10 rounded-xl p-3">
                <div className="text-sm text-white/60 mb-1">Тариф</div>
                <div className="text-2xl font-bold text-yellow-300">
                  {currentTariff.activeTariff || 'Бесплатный'}
                </div>
                {!currentTariff.activeTariff && (
                  <div className="text-xs text-white/50 mt-1">5 GB бесплатно</div>
                )}
              </div>
              <div className="bg-white/10 rounded-xl p-3">
                <div className="text-sm text-white/60 mb-1">Бесплатно</div>
                <div className="text-lg font-bold text-blue-300">
                  {formatBytes(currentTariff.freeStorageLimit || 5 * 1024 * 1024 * 1024)}
                </div>
              </div>
              <div className="bg-white/10 rounded-xl p-3">
                <div className="text-sm text-white/60 mb-1">Платный лимит</div>
                <div className="text-lg font-bold text-purple-300">
                  {currentTariff.activeTariff
                    ? formatBytes(currentTariff.totalStorageLimit - currentTariff.freeStorageLimit)
                    : '—'}
                </div>
              </div>
              <div className="bg-white/10 rounded-xl p-3">
                <div className="text-sm text-white/60 mb-1">Использовано</div>
                <div className="text-lg font-bold text-blue-300">
                  {formatBytes(usedStorage || 0)}
                </div>
              </div>
              <div className="bg-white/10 rounded-xl p-3">
                <div className="text-sm text-white/60 mb-1">Всего</div>
                <div className="text-lg font-bold text-green-300">
                  {formatBytes(currentTariff.totalStorageLimit || 5 * 1024 * 1024 * 1024)}
                </div>
              </div>
            </div>

            {/* Дата окончания тарифа */}
            {currentTariff.activeTariff && currentTariff.tariffEndDate && (
              <div className="mt-3 p-2 bg-white/5 rounded-lg">
                <div className="flex items-center justify-between">
                  <span className="text-white/70">Действует до:</span>
                  <span className="font-medium text-white">
                    {formatDate(currentTariff.tariffEndDate)}
                    {currentTariff.daysLeft > 0 && (
                      <span className="ml-2 text-sm text-white/50">
                        (осталось {currentTariff.daysLeft} дн.)
                      </span>
                    )}
                  </span>
                </div>
              </div>
            )}

            {/* Прогресс-бар */}
            <div className="mt-3">
              <div className="flex justify-between text-sm mb-1">
                <span className="text-white/80">Заполнено</span>
                <span className="text-blue-300">{calculatePercentage()}%</span>
              </div>
              <div className="w-full bg-gray-700/50 rounded-full h-2 overflow-hidden">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${getProgressBarColor(calculatePercentage())}`}
                  style={{ width: `${Math.min(calculatePercentage(), 100)}%` }}
                />
              </div>
            </div>

            {/* Переключатель автопродления */}
            {currentTariff.activeTariff && (
              <div className="mt-3 p-3 bg-white/5 rounded-xl border border-white/10">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    <div>
                      <span className="font-medium text-white">Автопродление</span>
                      <p className="text-sm text-white/50">
                        {currentTariff.autoRenew
                          ? 'Тариф будет автоматически продлеваться каждый месяц'
                          : 'Тариф не будет продлен автоматически'}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={handleToggleAutoRenew}
                    disabled={purchasing}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:ring-offset-gray-900 ${
                      currentTariff.autoRenew ? 'bg-green-600' : 'bg-gray-600'
                    } ${purchasing ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                    role="switch"
                    aria-checked={currentTariff.autoRenew}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        currentTariff.autoRenew ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Сообщения */}
        {error && (
          <div className="mb-3 p-2 bg-red-500/20 border border-red-500 rounded-xl text-center text-sm">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-3 p-2 bg-green-500/20 border border-green-500 rounded-xl text-center text-sm">
            {success}
          </div>
        )}

        {loading && (
          <div className="mb-3 p-2 bg-blue-500/20 border border-blue-500 rounded-xl text-center text-sm">
            Загрузка информации о тарифе...
          </div>
        )}

        {/* Сетка тарифов */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-auto">
          {tariffs.map((tariff) => (
            <div
              key={tariff.id}
              className={`bg-white/10 backdrop-blur-xl rounded-2xl p-5 border-2 transition-all duration-300 hover:scale-105 ${
                currentTariff?.activeTariff === tariff.id
                  ? 'border-yellow-400 shadow-lg shadow-yellow-500/20'
                  : 'border-white/10 hover:border-blue-500/50'
              }`}
            >
              <h2 className="text-2xl font-bold mb-2">{tariff.name}</h2>
              <div className="text-3xl font-bold text-yellow-300 mb-4">{tariff.price}</div>
              <div className="text-xl text-blue-300 mb-6">{tariff.storage}</div>

              <ul className="space-y-3 mb-8 flex-grow">
                  {tariff.features.map((feature, index) => (
                    <li key={index} className="flex items-center text-white/80">
                      {feature && (
                        <>
                          <svg className="w-5 h-5 mr-2 text-green-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                          </svg>
                          <span>{feature}</span>
                        </>
                      )}
                    </li>
                  ))}
                </ul>

              <button
                onClick={() => handlePurchase(tariff.id)}
                disabled={purchasing || currentTariff?.activeTariff === tariff.id}
                className={`w-full py-3 rounded-xl font-medium transition-all ${
                  currentTariff?.activeTariff === tariff.id
                    ? 'bg-green-600/50 cursor-not-allowed'
                    : 'bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-500 hover:to-cyan-500'
                }`}
              >
                {purchasing && selectedTariff?.id === tariff.id ? (
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white mr-2"></div>
                    <span>Обработка...</span>
                  </div>
                ) : currentTariff?.activeTariff === tariff.id ? (
                  'Текущий тариф'
                ) : (
                  'Выбрать'
                )}
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Модальное окно оплаты */}
      <PaymentModal
        isOpen={isPaymentModalOpen}
        onClose={() => {
          setIsPaymentModalOpen(false);
          setSelectedTariff(null);
        }}
        tariff={selectedTariff}
        onConfirm={handleConfirmPayment}
      />
    </div>
  );
}