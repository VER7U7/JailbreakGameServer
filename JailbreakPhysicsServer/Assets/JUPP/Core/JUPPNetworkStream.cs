using System;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace JUPP
{
    public static class JUPPNetworkStream
    {
        /// <summary>
        /// Синхронно читает точно указанное количество байтов из потока.
        /// Поведение аналогично DataInputStream.readFully() в Java.
        /// </summary>
        /// <param name="stream">Экземпляр NetworkStream, из которого нужно читать.</param>
        /// <param name="buffer">Буфер, в который будут записаны прочитанные байты.</param>
        /// <param name="offset">Смещение в буфере, с которого нужно начать запись.</param>
        /// <param name="count">Точное количество байтов, которое нужно прочитать.</param>
        /// <param name="cancellationToken">Токен отмены для прерывания операции.</param>
        /// <exception cref="EndOfStreamException">Выбрасывается, если соединение закрывается удаленной стороной.</exception>
        /// <exception cref="IOException">Выбрасывается при ошибках ввода/вывода.</exception>
        /// <exception cref="OperationCanceledException">Выбрасывается, если операция отменена.</exception>
        public static void ReadExactly(
            this NetworkStream stream,
            byte[] buffer,
            int offset,
            int count,
            CancellationToken cancellationToken = default)
        {
            int totalBytesRead = 0;
            while (totalBytesRead < count)
            {
                cancellationToken.ThrowIfCancellationRequested();

                // Read возвращает количество фактически прочитанных байтов.
                // Оно может быть меньше count - totalBytesRead.
                int bytesRead = stream.Read(buffer, offset + totalBytesRead, count - totalBytesRead);

                if (bytesRead == 0)
                {
                    // Если Read возвращает 0, это означает, что удаленная сторона
                    // корректно закрыла соединение или поток ввода/вывода.
                    throw new EndOfStreamException($"Соединение закрыто удаленной стороной до чтения {count} байт.");
                }

                totalBytesRead += bytesRead;
            }
        }

        /// <summary>
        /// Синхронно читает точно указанное количество байтов из потока и возвращает их в новом массиве.
        /// </summary>
        /// <param name="stream">Экземпляр NetworkStream, из которого нужно читать.</param>
        /// <param name="count">Точное количество байтов, которое нужно прочитать.</param>
        /// <param name="cancellationToken">Токен отмены для прерывания операции.</param>
        /// <returns>Массив байтов, содержащий прочитанные данные.</returns>
        /// <exception cref="EndOfStreamException">Выбрасывается, если соединение закрывается удаленной стороной.</exception>
        /// <exception cref="IOException">Выбрасывается при ошибках ввода/вывода.</exception>
        /// <exception cref="OperationCanceledException">Выбрасывается, если операция отменена.</exception>
        public static byte[] ReadExactly(
            this NetworkStream stream,
            int count,
            CancellationToken cancellationToken = default)
        {
                byte[] buffer = new byte[count];
                stream.ReadExactly(buffer, 0, count, cancellationToken);
                return buffer;
        }
    }
}