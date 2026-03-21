package org.smalwe;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;

import java.util.Collections;

public class MilvusDocStorage {
    public static void main(String[] args) {
        // 1. Connect to Milvus
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(System.getenv("MILVUS_URI")) // Replace with your IP
                .build();
        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        String collectionName = "agent_documents";

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
                .dimension(1536) // Standard for OpenAI (text-embedding-3-small)
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
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(Collections.singletonList(indexParam))
                .build();

        client.createCollection(request);
        System.out.println("Collection '" + collectionName + "' created and indexed successfully.");
    }
}