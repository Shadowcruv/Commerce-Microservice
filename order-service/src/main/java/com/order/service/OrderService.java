package com.order.service;


import com.order.dto.InventoryResponse;
import com.order.dto.OrderLineItemDto;
import com.order.dto.OrderRequest;
import com.order.event.OrderPlacedEvent;
import com.order.model.Order;
import com.order.model.OrderLineItem;
import com.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItem> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream().map(this::mapToDto).toList();

        order.setOrderLineItemsList(orderLineItems);
        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItem::getSkuCode).toList();

        Span inventoryServiceLookUp = tracer.nextSpan().name("inventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookUp.start())) {
            //Call inventory service and place order if product is in stock
            InventoryResponse[] inventoryResponses =  webClientBuilder.build().get()
                    .uri("lb://inventory-service/api/v1/inventory",
                            uriBuilder -> uriBuilder.queryParam("skucode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block(); //Block is used so that the call would be synchronous

//         Above we also changed localhost:8083 to inventory-service(application name in the discovery server) in
//         the uri to enable flexibility and also inventory-service is
//         name the discovery server used to hold different instances of the inventory-service

//        List<InventoryResponse> responses = Arrays.stream(inventoryResponses).filter(inventoryResponse -> !inventoryResponse.getIsInStock()).toList();

            boolean result =  Arrays.stream(inventoryResponses).allMatch(InventoryResponse::getIsInStock);


            if(result){
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order placed successfully";
            }else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

        }finally {
            inventoryServiceLookUp.end();
        }

    }

    private OrderLineItem mapToDto(OrderLineItemDto orderLineItemDto){
        return OrderLineItem.builder()
                .price(orderLineItemDto.getPrice())
                .skuCode(orderLineItemDto.getSkuCode())
                .quantity(orderLineItemDto.getQuantity())
                .build();
    }

    public List<Order> getTestOrder(){
        return orderRepository.findAll();
    }

}
