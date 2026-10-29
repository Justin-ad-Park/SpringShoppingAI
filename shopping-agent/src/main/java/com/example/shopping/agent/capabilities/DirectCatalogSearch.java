package com.example.shopping.agent.capabilities;

import java.util.List;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

public class DirectCatalogSearch implements CatalogSearchStrategy {
    public List<Product> search(String message, ProductQuery query, Catalog catalog) { return catalog.search(query); }
    public String name() { return "WORKFLOW"; }
}
