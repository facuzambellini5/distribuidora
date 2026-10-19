package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.ProductUnit;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.OrderItem;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductUnitMappingTest {

    private Product product(ProductUnit unit) {
        Product product = new Product();
        product.setId(7L);
        product.setName("Harina");
        product.setUnit(unit);
        return product;
    }

    @Test
    void orderItemResponseExposesProductUnit() {
        OrderItem item = new OrderItem();
        item.setProduct(product(ProductUnit.KILOGRAM));

        assertEquals(ProductUnit.KILOGRAM, new OrderItemResponse(item).productUnit());
    }

    @Test
    void deliveryStockResponseExposesProductUnit() {
        DeliveryStock stock = new DeliveryStock();
        stock.setTruck(new Truck());
        stock.setProduct(product(ProductUnit.LITER));

        assertEquals(ProductUnit.LITER, new DeliveryStockResponse(stock).productUnit());
    }
}
