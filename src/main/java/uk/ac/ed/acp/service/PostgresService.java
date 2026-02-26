package uk.ac.ed.acp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.List;

@Service
public class PostgresService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<JsonNode> getAllRows(String table) {
        String sql = "SELECT * FROM " + sanitizeTableName(table);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            ObjectNode node = objectMapper.createObjectNode();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String colName = meta.getColumnName(i);
                int colType = meta.getColumnType(i);
                Object value = rs.getObject(i);

                if (rs.wasNull() || value == null) {
                    node.putNull(colName);
                } else if (value instanceof Integer) {
                    node.put(colName, (Integer) value);
                } else if (value instanceof Long) {
                    node.put(colName, (Long) value);
                } else if (value instanceof Double) {
                    node.put(colName, (Double) value);
                } else if (value instanceof Float) {
                    node.put(colName, (Float) value);
                } else if (value instanceof Boolean) {
                    node.put(colName, (Boolean) value);
                } else if (value instanceof java.math.BigDecimal) {
                    node.put(colName, ((java.math.BigDecimal) value).doubleValue());
                } else {
                    node.put(colName, value.toString());
                }
            }
            return (JsonNode) node;
        });
    }

    public void insertRow(String table, JsonNode droneJson) {
        // The table has flattened columns matching the enriched drone structure.
        // Columns: name, id, cooling, heating, capacity, maxmoves,
        //          costpermove, costinitial, costfinal, costper100moves
        // (Postgres lowercases unquoted identifiers)
        JsonNode cap = droneJson.get("capability");

        String sql = "INSERT INTO " + sanitizeTableName(table)
                + " (name, id, cooling, heating, capacity, maxmoves, "
                + "costpermove, costinitial, costfinal, costper100moves) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                droneJson.get("name").asText(),
                droneJson.get("id").asText(),
                cap.get("cooling").asBoolean(),
                cap.get("heating").asBoolean(),
                cap.get("capacity").asInt(),
                cap.get("maxMoves").asInt(),
                cap.get("costPerMove").asDouble(),
                cap.get("costInitial").asDouble(),
                cap.get("costFinal").asDouble(),
                droneJson.get("costPer100Moves").asDouble()
        );
    }

    private String sanitizeTableName(String table) {
        return table.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
