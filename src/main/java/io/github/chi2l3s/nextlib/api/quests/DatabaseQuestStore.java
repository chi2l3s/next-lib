package io.github.chi2l3s.nextlib.api.quests;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public final class DatabaseQuestStore implements QuestStore {
    private final DatabaseClient client;
    private final String tableName;

    public DatabaseQuestStore(DatabaseClient client) {
        this(client, "nextlib_quest_progress");
    }

    public DatabaseQuestStore(DatabaseClient client, String tableName) {
        this.client = Objects.requireNonNull(client, "client");
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        ensureSchema();
    }

    @Override
    public void saveProgress(QuestProgress progress) {
        Objects.requireNonNull(progress, "progress");
        client.withConnection(connection -> {
            try {
                connection.setAutoCommit(false);
                deleteExisting(progress, connection);
                insertProgress(progress, connection);
                connection.commit();
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new DatabaseException("Failed to save quest progress", e);
            } finally {
                restoreAutoCommit(connection);
            }
            return null;
        });
    }

    @Override
    public Optional<QuestProgress> loadProgress(UUID playerId, Quest quest) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(quest, "quest");
        Map<String, Double> rows = new HashMap<>();
        String sql = "SELECT objective_id, progress FROM " + tableName + " WHERE player_uuid = ? AND quest_id = ?";
        client.query(sql, statement -> {
                    statement.setString(1, playerId.toString());
                    statement.setString(2, quest.getId());
                }, resultSet -> new ProgressRow(resultSet.getString("objective_id"), resultSet.getDouble("progress")))
                .forEach(row -> rows.put(row.objectiveId(), row.progress()));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new QuestProgress(quest, playerId, rows));
    }

    @Override
    public List<QuestProgress> loadAll(UUID playerId, Function<String, Optional<Quest>> questResolver) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(questResolver, "questResolver");
        String sql = "SELECT quest_id, objective_id, progress FROM " + tableName + " WHERE player_uuid = ?";
        Map<String, Map<String, Double>> rows = new HashMap<>();
        client.query(sql, statement -> statement.setString(1, playerId.toString()),
                resultSet -> new QuestRow(resultSet.getString("quest_id"),
                        resultSet.getString("objective_id"),
                        resultSet.getDouble("progress")))
                .forEach(row -> rows
                        .computeIfAbsent(row.questId(), ignored -> new HashMap<>())
                        .put(row.objectiveId(), row.progress()));
        List<QuestProgress> progressList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : rows.entrySet()) {
            questResolver.apply(entry.getKey()).ifPresent(quest ->
                    progressList.add(new QuestProgress(quest, playerId, entry.getValue())));
        }
        return progressList;
    }

    private void ensureSchema() {
        String ddl = "CRETE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "quest_id VARCHAR(128) NOT NULL, " +
                "objective_id VARCHAR(128) NOT NULL, " +
                "progress DOUBLE NOT NULL, " +
                "target DOUBLE NOT NULL, " +
                "PRIMARY KEY(player_uuid, quest_id, objective_id)" +
                ");";
        client.execute(ddl, null);
    }

    private void deleteExisting(QuestProgress progress, Connection connection) throws SQLException {
        String delete = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND quest_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(delete)) {
            statement.setString(1, progress.getPlayerId().toString());
            statement.setString(2, progress.getQuest().getId());
            statement.executeUpdate();
        }
    }

    private void insertProgress(QuestProgress progress, Connection connection) throws SQLException {
        String insert = "INSERT INTO " + tableName + " (player_uuid, quest_id, objective_id, progress, target) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (Map.Entry<String, Double> entry : progress.getAllProgress().entrySet()) {
                statement.setString(1, progress.getPlayerId().toString());
                statement.setString(2, progress.getQuest().getId());
                statement.setString(3, entry.getKey());
                statement.setDouble(4, entry.getValue());
                QuestObjective objective = progress.getQuest().getObjectives().stream()
                        .filter(o -> o.getId().equals(entry.getKey()))
                        .findFirst()
                        .orElse(null);
                double target = objective != null ? objective.getTargetAmount() : entry.getValue();
                statement.setDouble(5, target);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }

    private record ProgressRow(String objectiveId, double progress) {
    }

    private record QuestRow(String questId, String objectiveId, double progress) {
    }
}
