package hr.fer.dipl.service;

import hr.fer.dipl.db.model.OrderStatus;
import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.OrderRequest;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderRequest orderRequest);

    OrderDTO getOrderById(Long orderId);

    List<OrderDTO> getAllOrders();

    List<OrderDTO> getOrdersByUserId(Long userId);

    OrderDTO updateOrderStatus(Long orderId, OrderStatus status);
}
