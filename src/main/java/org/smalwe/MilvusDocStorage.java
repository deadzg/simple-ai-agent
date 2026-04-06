package org.smalwe;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;

import java.util.Collections;

public class MilvusDocStorage {
    private MilvusClientV2 client;
    private static final String AGENT_DOC_COLLECTION = "agent_documents";

    private void initClient() {
        // 1. Connect to Milvus
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(System.getenv("MILVUS_URI")) // Replace with your IP
                .build();
        client = new MilvusClientV2(connectConfig);
    }

    public static void main(String[] args) {

        MilvusDocStorage milvusDocStorage = new MilvusDocStorage();
        milvusDocStorage.initClient();

        milvusDocStorage.dropCollection(AGENT_DOC_COLLECTION);
    }

    private void dropCollection(String collectionName) {
        DropCollectionReq dropQuickSetupParam = DropCollectionReq.builder()
                .collectionName(collectionName)
                .build();

        client.dropCollection(dropQuickSetupParam);
        System.out.println("Collection dropped:" + collectionName);
    }

    private void createCollection(String collectionName) {
        // 2. Define Schema
        CreateCollectionReq.CollectionSchema schema = client.createSchema();
        schema.setEnableDynamicField(true); // Allows for flexible metadata

        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(true)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("vector")
                .dataType(DataType.FloatVector)
                .dimension(3072) // Match this to your Google Embedding model output
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_text")
                .dataType(DataType.VarChar)
                .maxLength(65535) // Large enough for a text paragraph
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("file_name")
                .dataType(DataType.VarChar)
                .maxLength(500)
                .build());

        // 3. Prepare Index Parameters
        IndexParam indexParam = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .build();

        // 4. Create Collection
        CreateCollectionReq request = CreateCollectionReq.builder()
                .collectionName(AGENT_DOC_COLLECTION)
                .collectionSchema(schema)
                .indexParams(Collections.singletonList(indexParam))
                .build();

        client.createCollection(request);
        System.out.println("Collection '" + AGENT_DOC_COLLECTION + "' created and indexed successfully.");
    }
}