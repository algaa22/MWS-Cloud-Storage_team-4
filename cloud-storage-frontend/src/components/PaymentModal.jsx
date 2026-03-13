import React, { useState } from 'react';

export default function PaymentModal({ isOpen, onClose, tariff, onConfirm }) {
  const [paymentMethod, setPaymentMethod] = useState('card');
  const [autoRenew, setAutoRenew] = useState(true);
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await onConfirm(paymentMethod, autoRenew);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Оплата тарифа</h2>

        {/* Информация о тарифе */}
        <div className="mb-6">
          <p className="text-white/70">Вы выбрали тариф:</p>
          <p className="text-xl font-bold text-yellow-300">{tariff?.name}</p>
          <p className="text-2xl font-bold mt-2">{tariff?.price}</p>
        </div>

        {/* Способ оплаты */}
        <div className="space-y-4 mb-6">
          <p className="text-sm text-white/50 mb-2">Способ оплаты</p>

          <label className="flex items-center space-x-3 p-3 bg-white/10 rounded-xl cursor-pointer">
            <input
              type="radio"
              name="payment"
              value="card"
              checked={paymentMethod === 'card'}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="w-4 h-4"
            />
            <div>
              <span className="font-medium">Банковская карта</span>
              <p className="text-sm text-white/50">Visa, Mastercard, МИР</p>
            </div>
          </label>

          <label className="flex items-center space-x-3 p-3 bg-white/10 rounded-xl cursor-pointer">
            <input
              type="radio"
              name="payment"
              value="sbp"
              checked={paymentMethod === 'sbp'}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="w-4 h-4"
            />
            <div>
              <span className="font-medium">СБП</span>
              <p className="text-sm text-white/50">Система быстрых платежей</p>
            </div>
          </label>
        </div>

        {/* Автопродление */}
        <div className="mb-6 p-4 bg-white/5 rounded-xl border border-white/10">
          <div className="flex items-center justify-between">
            <div>
              <span className="font-medium text-white">Автопродление</span>
              <p className="text-sm text-white/50 mt-1">
                {autoRenew
                  ? 'Тариф будет автоматически продлеваться каждый месяц'
                  : 'Тариф не будет продлен автоматически'}
              </p>
            </div>

            {/* Переключатель (toggle) */}
            <button
              type="button"
              onClick={() => setAutoRenew(!autoRenew)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                autoRenew ? 'bg-green-600' : 'bg-gray-600'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  autoRenew ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Дополнительная информация */}
          {autoRenew && (
            <p className="text-xs text-white/40 mt-3 pt-3 border-t border-white/10">
              ✓ Оплата будет списываться автоматически
              ✓ Можно отключить в любой момент
              ✓ Придет уведомление о продлении
            </p>
          )}
        </div>

        {/* Кнопки */}
        <div className="flex space-x-3">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 bg-white/20 hover:bg-white/30 rounded-xl transition-colors"
          >
            Отмена
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="flex-1 px-4 py-2 bg-gradient-to-r from-yellow-600 to-amber-600 hover:from-yellow-500 hover:to-amber-500 rounded-xl transition-all disabled:opacity-50 flex items-center justify-center"
          >
            {loading ? (
              <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white"></div>
            ) : (
              'Оплатить'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}