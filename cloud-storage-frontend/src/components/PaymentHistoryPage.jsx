import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { getPaymentHistory } from '../api';

export default function PaymentHistoryPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    fetchPaymentHistory();
  }, [token]);

  const fetchPaymentHistory = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await getPaymentHistory(token);
      setTransactions(data.transactions || []);
    } catch (err) {
      console.error("Error fetching payment history:", err);
      setError(err.message || "Ошибка загрузки истории платежей");
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '—';
    const date = new Date(dateString);
    const moscowDate = new Date(date.getTime() + (3 * 60 * 60 * 1000));

    return moscowDate.toLocaleString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    const date = new Date(dateString);
    const moscowDate = new Date(date.getTime() + (3 * 60 * 60 * 1000));

    return moscowDate.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  };

  const formatAmount = (amount) => {
    return new Intl.NumberFormat('ru-RU', {
      style: 'currency',
      currency: 'RUB',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'text-green-400 bg-green-500/20';
      case 'PENDING':
        return 'text-yellow-400 bg-yellow-500/20';
      case 'FAILED':
        return 'text-red-400 bg-red-500/20';
      default:
        return 'text-gray-400 bg-gray-500/20';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'Успешно';
      case 'PENDING':
        return 'В обработке';
      case 'FAILED':
        return 'Ошибка';
      default:
        return status;
    }
  };

  const getTariffPlanText = (plan) => {
    switch (plan) {
      case 'BASIC':
        return 'Базовый';
      case 'WORK':
        return 'Рабочий';
      case 'PREMIUM':
        return 'Премиум';
      default:
        return plan;
    }
  };

  const getTariffIcon = (plan) => {
    switch (plan) {
      case 'BASIC':
        return '⭐';
      case 'WORK':
        return '🌟';
      case 'PREMIUM':
        return '💎';
      default:
        return '📦';
    }
  };

  const calculateEndDate = (createdAt, durationDays) => {
    if (!createdAt) return null;
    const date = new Date(createdAt);
    const moscowDate = new Date(date.getTime() + (3 * 60 * 60 * 1000));
    const endDate = new Date(moscowDate);
    const days = durationDays || 30;
    endDate.setDate(endDate.getDate() + days);
    return endDate;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900">
      <div className="p-6">
        {/* Header как на странице тарифов */}
        <div className="flex justify-between items-center mb-8">
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
              История платежей
            </h1>
            <p className="text-white/60 text-sm mt-1">Все ваши транзакции</p>
          </div>

          <div className="w-20"></div>
        </div>

        {/* Content */}
        <div className="max-w-4xl mx-auto">
          {loading ? (
            <div className="flex flex-col justify-center items-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
              <span className="mt-4 text-white/70">Загрузка истории...</span>
            </div>
          ) : error ? (
            <div className="bg-red-500/20 border border-red-500 rounded-xl p-6 text-center backdrop-blur-sm">
              <div className="text-4xl mb-3">⚠️</div>
              <p className="text-red-400 mb-4">{error}</p>
              <button
                onClick={fetchPaymentHistory}
                className="px-5 py-2 bg-red-500/30 hover:bg-red-500/40 rounded-lg text-sm transition-colors"
              >
                Попробовать снова
              </button>
            </div>
          ) : transactions.length === 0 ? (
            <div className="text-center py-16 bg-white/10 backdrop-blur-sm rounded-2xl border border-white/20">
              <div className="text-7xl mb-4">💳</div>
              <h3 className="text-2xl font-semibold text-white mb-3">Нет платежей</h3>
              <p className="text-white/60 mb-6">
                У вас пока нет совершенных платежей.
              </p>
              <button
                onClick={() => navigate('/tariffs')}
                className="px-6 py-2.5 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 rounded-xl text-white transition-all shadow-lg"
              >
                Перейти к тарифам
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {transactions.map((transaction) => {
                const endDate = calculateEndDate(transaction.createdAt, transaction.durationDays);
                return (
                  <div
                    key={transaction.id}
                    className="bg-white/10 backdrop-blur-xl rounded-2xl p-5 border border-white/10 hover:border-blue-500/50 transition-all duration-300 hover:scale-[1.02]"
                  >
                    <div className="flex flex-wrap justify-between items-start gap-4">
                      {/* Левая часть */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3 flex-wrap mb-3">
                          <div className="flex items-center gap-2">
                            <span className="text-2xl">{getTariffIcon(transaction.tariffPlan)}</span>
                            <h3 className="text-xl font-semibold text-white">
                              {getTariffPlanText(transaction.tariffPlan)}
                            </h3>
                          </div>
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(transaction.status)}`}>
                            {getStatusText(transaction.status)}
                          </span>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                          <div className="space-y-2">
                            <div className="flex items-center gap-2 text-white/70">
                              <span className="text-base">📅</span>
                              <span>Покупка: <span className="text-white">{formatDateTime(transaction.createdAt)}</span></span>
                            </div>
                            <div className="flex items-center gap-2 text-white/70">
                              <span className="text-base">💾</span>
                              <span>Объём хранилища:
                                <span className="text-white font-medium ml-1">
                                  {transaction.storageLimitGb ? `${transaction.storageLimitGb} ГБ` : '—'}
                                </span>
                              </span>
                            </div>
                            <div className="flex items-center gap-2 text-white/70">
                              <span className="text-base">📆</span>
                              <span>Длительность:
                                <span className="text-white ml-1">
                                  {transaction.durationDays ? `${transaction.durationDays} дней` : '30 дней'}
                                </span>
                              </span>
                            </div>
                            <div className="flex items-center gap-2 text-white/70">
                              <span className="text-base">🔄</span>
                              <span>Автопродление:
                                <span className={`ml-1 ${transaction.autoRenew ? 'text-green-400' : 'text-red-400'}`}>
                                  {transaction.autoRenew !== undefined ? (transaction.autoRenew ? 'Включено' : 'Отключено') : '—'}
                                </span>
                              </span>
                            </div>
                          </div>
                          <div className="space-y-2">
                            <div className="flex items-center gap-2 text-white/70">
                              <span className="text-base">⏰</span>
                              <span>Окончание подписки:
                                <span className="text-white ml-1">
                                  {formatDate(endDate?.toISOString())}
                                </span>
                              </span>
                            </div>
                            {transaction.paymentMethod && (
                              <div className="flex items-center gap-2 text-white/70">
                                <span className="text-base">💳</span>
                                <span>Оплата: <span className="text-white">{transaction.paymentMethod === 'card' ? 'Банковская карта' : transaction.paymentMethod}</span></span>
                              </div>
                            )}
                            {transaction.completedAt && transaction.status === 'COMPLETED' && (
                              <div className="flex items-center gap-2 text-white/70">
                              </div>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Правая часть - сумма */}
                      <div className="text-right border-l border-white/20 pl-6">
                        <div className="text-3xl font-bold text-yellow-300">
                          {formatAmount(transaction.price || transaction.amount)}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}