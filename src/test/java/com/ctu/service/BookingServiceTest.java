
package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BookingService bookingService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("R01", 500000, true);
    }

    // 1. Dat phong thanh cong
    @Test
    void testBookRoom_Success() {
        when(roomRepository.findById("R01"))
                .thenReturn(Optional.of(room));
        when(paymentGateway.charge("123456", 1000000))
                .thenReturn(true);

        BookingReceipt receipt =
                bookingService.bookRoom("R01", 2, "123456");

        assertNotNull(receipt);
        assertEquals("R01", receipt.getRoomId());
        assertEquals(1000000, receipt.getTotalAmount());
        assertFalse(room.isAvailable());

        verify(paymentGateway, times(1))
                .charge("123456", 1000000);
        verify(roomRepository, times(1)).save(room);
    }

    // 2. Khong tim thay phong
    @Test
    void testBookRoom_RoomNotFound_ThrowsException() {
        when(roomRepository.findById("R99"))
                .thenReturn(Optional.empty());

        assertThrows(RoomNotAvailableException.class,
                () -> bookingService.bookRoom("R99", 1, "123456"));

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
        verify(roomRepository, never()).save(any(Room.class));
    }

    // 3. Phong da duoc dat
    @Test
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        room.setAvailable(false);
        when(roomRepository.findById("R01"))
                .thenReturn(Optional.of(room));

        assertThrows(RoomNotAvailableException.class,
                () -> bookingService.bookRoom("R01", 1, "123456"));

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
        verify(roomRepository, never()).save(any(Room.class));
    }

    // 4. Thanh toan that bai
    @Test
    void testBookRoom_PaymentFailed_ThrowsException() {
        when(roomRepository.findById("R01"))
                .thenReturn(Optional.of(room));
        when(paymentGateway.charge("123456", 500000))
                .thenReturn(false);

        assertThrows(PaymentFailedException.class,
                () -> bookingService.bookRoom("R01", 1, "123456"));

        assertTrue(room.isAvailable());
        verify(paymentGateway, times(1))
                .charge("123456", 500000);
        verify(roomRepository, never()).save(any(Room.class));
    }

    // 5. So dem khong hop le
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void testBookRoom_InvalidNights_ThrowsException(int nights) {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom("R01", nights, "123456"));

        verifyNoInteractions(roomRepository, paymentGateway);
    }

    // 6. The thanh toan null hoac rong
    @Test
    void testBookRoom_NullCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom("R01", 1, null));

        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testBookRoom_EmptyCreditCard_ThrowsException(String card) {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom("R01", 1, card));

        verifyNoInteractions(roomRepository, paymentGateway);
    }

    // 7. Ma phong null
    @Test
    void testBookRoom_NullRoomId_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom(null, 1, "123456"));

        verifyNoInteractions(roomRepository, paymentGateway);
    }
}
