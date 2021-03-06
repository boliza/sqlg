package org.umlg.sqlg.structure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A transaction scoped cache.
 * Date: 2014/10/04
 * Time: 3:20 PM
 */
public class TransactionCache {

    private Connection connection;
    private List<ElementPropertyRollback> elementPropertyRollbackFunctions;
    private BatchManager batchManager;
    private Map<RecordId, SqlgVertex> vertexCache = new HashMap<>();
    //true if a schema modification statement has been executed.
    //it is important to know this as schema modification creates exclusive locks.
    //In particular it locks querying the schema itself.
    private boolean schemaModification = false;

    static TransactionCache of(Connection connection, List<ElementPropertyRollback> elementPropertyRollbackFunctions, BatchManager batchManager) {
        return new TransactionCache(connection, elementPropertyRollbackFunctions, batchManager);
    }

    private TransactionCache(
            Connection connection,
            List<ElementPropertyRollback> elementPropertyRollbackFunctions,
            BatchManager batchManager) {

        this.connection = connection;
        this.elementPropertyRollbackFunctions = elementPropertyRollbackFunctions;
        this.batchManager = batchManager;
    }

    Connection getConnection() {
        return this.connection;
    }

    List<ElementPropertyRollback> getElementPropertyRollback() {
        return this.elementPropertyRollbackFunctions;
    }

    BatchManager getBatchManager() {
        return this.batchManager;
    }

    void clear() {
        this.elementPropertyRollbackFunctions.clear();
        this.batchManager.clear();
        this.vertexCache.clear();
        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    SqlgVertex putVertexIfAbsent(SqlgGraph sqlgGraph, RecordId recordId) {
        if (!this.vertexCache.containsKey(recordId)) {
            SqlgVertex sqlgVertex = new SqlgVertex(sqlgGraph, recordId.getId(), recordId.getSchemaTable().getSchema(), recordId.getSchemaTable().getTable());
            this.vertexCache.put(recordId, sqlgVertex);
            return sqlgVertex;
        } else {
            return this.vertexCache.get(recordId);
        }
    }

    SqlgVertex putVertexIfAbsent(SqlgVertex sqlgVertex) {
        if (!this.vertexCache.containsKey(sqlgVertex.id())) {
            this.vertexCache.put((RecordId)sqlgVertex.id(), sqlgVertex);
            return sqlgVertex;
        } else {
            return this.vertexCache.get(sqlgVertex.id());
        }
    }

    void add(SqlgVertex sqlgVertex) {
        if (this.vertexCache.containsKey(sqlgVertex.id())) {
            throw new IllegalStateException("The vertex cache should never already contain a new vertex!");
        } else {
            this.vertexCache.put((RecordId) sqlgVertex.id(), sqlgVertex);
        }
    }

    boolean isSchemaModification() {
        return schemaModification;
    }

    void setSchemaModification(boolean schemaModification) {
        this.schemaModification = schemaModification;
    }

}
