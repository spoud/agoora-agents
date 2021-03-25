package io.spoud.agoora.agents.openapi.repository;

import io.spoud.sdm.logistics.domain.v1.DataItem;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DataItemRepository {
  private final Map<String, DataItem> itemsById = new HashMap<>();

  public void update(DataItem dataItem) {
    itemsById.put(dataItem.getId(), dataItem);
  }

  public void deleteById(String id) {
    itemsById.remove(id);
  }

  public List<DataItem> findAll() {
    return new ArrayList<>(itemsById.values());
  }

  public void clear() {
    itemsById.clear();
  }
}
