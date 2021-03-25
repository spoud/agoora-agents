package io.spoud.agoora.agents.openapi.repository;

import io.spoud.sdm.logistics.domain.v1.DataPort;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DataPortRepository {
  private final Map<String, DataPort> portsById = new HashMap<>();

  public void update(DataPort dataPort) {
    portsById.put(dataPort.getId(), dataPort);
  }

  public void deleteById(String id) {
    portsById.remove(id);
  }

  public List<DataPort> findAll() {
    return new ArrayList<>(portsById.values());
  }

  public void clear() {
    portsById.clear();
  }
}
