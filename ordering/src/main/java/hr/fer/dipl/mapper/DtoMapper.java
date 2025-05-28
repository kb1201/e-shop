package hr.fer.dipl.mapper;

import hr.fer.dipl.db.model.CartItem;
import hr.fer.dipl.db.model.Order;
import hr.fer.dipl.db.model.OrderItem;
import hr.fer.dipl.dto.CartItemDTO;
import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.OrderItemDTO;

import java.util.List;
import java.util.stream.Collectors;

public class DtoMapper {

    public static CartItemDTO toCartItemDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProductId());
        dto.setProductName(cartItem.getProductName());
        dto.setPrice(cartItem.getPrice());
        dto.setQuantity(cartItem.getQuantity());
        return dto;
    }

    public static OrderDTO toOrderDTO(Order order, List<OrderItem> orderItems) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setBillingAddress(order.getBillingAddress());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemDTO> itemDTOs = orderItems.stream()
                .map(DtoMapper::toOrderItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    public static OrderItemDTO toOrderItemDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setProductId(orderItem.getProductId());
        dto.setProductName(orderItem.getProductName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setUnitPrice(orderItem.getUnitPrice());
        return dto;
    }
}
