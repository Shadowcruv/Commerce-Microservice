package com.inventory.service;

import com.inventory.dto.InventoryResponse;
import com.inventory.model.Inventory;
import com.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    @SneakyThrows //Don't use this in production. You use try and catch exception or throw your exception
    public List<InventoryResponse> isInStock(List<String> stuCodes){
        log.info("Wait started");
        Thread.sleep(10000);
        log.info("Wait finished");
        return inventoryRepository.findBySkuCodeIn(stuCodes).stream()
                .map(this::mapToInventoryResponse)
                .toList();
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .stuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() > 0)
                .build();
    }

    public List<InventoryResponse> getAll() {
       return inventoryRepository.findAll().
               stream()
               .map(this::mapToInventoryResponse)
               .toList();
    }
}
