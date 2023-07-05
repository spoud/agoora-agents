package io.spoud.agoora.agents.pgsql.repository;

import io.spoud.sdm.logistics.domain.v1.DataItem;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DataItemRepository {
  private final Map<String, DataItem> itemsById = new HashMap<>();

  public void update(DataItem state) {
    itemsById.put(state.getId(), state);
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
