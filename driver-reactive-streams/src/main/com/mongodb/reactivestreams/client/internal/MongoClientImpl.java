/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.reactivestreams.client.internal;

import com.mongodb.ClientSessionOptions;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncClientSession;
import com.mongodb.internal.async.client.AsyncMongoClient;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.ListDatabasesPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;

import static com.mongodb.assertions.Assertions.notNull;


/**
 * The internal MongoClient implementation.
 *
 * <p>This should not be considered a part of the public API.</p>
 */
public final class MongoClientImpl implements MongoClient {
    private final AsyncMongoClient wrapped;

    /**
     * The internal MongoClientImpl constructor.
     *
     * <p>This should not be considered a part of the public API.</p>
     *
     * @param wrapped the underlying MongoClient
     */
    public MongoClientImpl(final AsyncMongoClient wrapped) {
        this.wrapped = notNull("wrapped", wrapped);
    }

    @Override
    public MongoDatabase getDatabase(final String name) {
        return new MongoDatabaseImpl(wrapped.getDatabase(name));
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public Publisher<String> listDatabaseNames() {
        return Publishers.publish(wrapped.listDatabaseNames());
    }

    @Override
    public Publisher<String> listDatabaseNames(final ClientSession clientSession) {
        return Publishers.publish(wrapped.listDatabaseNames(clientSession.getWrapped()));
    }

    @Override
    public ListDatabasesPublisher<Document> listDatabases() {
        return listDatabases(Document.class);
    }

    @Override
    public <TResult> ListDatabasesPublisher<TResult> listDatabases(final Class<TResult> clazz) {
        return new ListDatabasesPublisherImpl<>(wrapped.listDatabases(clazz));
    }

    @Override
    public ListDatabasesPublisher<Document> listDatabases(final ClientSession clientSession) {
        return listDatabases(clientSession, Document.class);
    }

    @Override
    public <TResult> ListDatabasesPublisher<TResult> listDatabases(final ClientSession clientSession, final Class<TResult> clazz) {
        return new ListDatabasesPublisherImpl<>(wrapped.listDatabases(clientSession.getWrapped(), clazz));
    }

    @Override
    public ChangeStreamPublisher<Document> watch() {
        return watch(Collections.emptyList());
    }

    @Override
    public <TResult> ChangeStreamPublisher<TResult> watch(final Class<TResult> resultClass) {
        return watch(Collections.emptyList(), resultClass);
    }

    @Override
    public ChangeStreamPublisher<Document> watch(final List<? extends Bson> pipeline) {
        return watch(pipeline, Document.class);
    }

    @Override
    public <TResult> ChangeStreamPublisher<TResult> watch(final List<? extends Bson> pipeline, final Class<TResult> resultClass) {
        return new ChangeStreamPublisherImpl<>(wrapped.watch(pipeline, resultClass));
    }

    @Override
    public ChangeStreamPublisher<Document> watch(final ClientSession clientSession) {
        return watch(clientSession, Collections.emptyList(), Document.class);
    }

    @Override
    public <TResult> ChangeStreamPublisher<TResult> watch(final ClientSession clientSession, final Class<TResult> resultClass) {
        return watch(clientSession, Collections.emptyList(), resultClass);
    }

    @Override
    public ChangeStreamPublisher<Document> watch(final ClientSession clientSession, final List<? extends Bson> pipeline) {
        return watch(clientSession, pipeline, Document.class);
    }

    @Override
    public <TResult> ChangeStreamPublisher<TResult> watch(final ClientSession clientSession, final List<? extends Bson> pipeline,
                                                          final Class<TResult> resultClass) {
        notNull("clientSession", clientSession);
        return new ChangeStreamPublisherImpl<>(wrapped.watch(clientSession.getWrapped(), pipeline, resultClass));
    }

    @Override
    public Publisher<ClientSession> startSession() {
        return startSession(ClientSessionOptions.builder().build());
    }

    @Override
    public Publisher<ClientSession> startSession(final ClientSessionOptions options) {
        return Publishers.publish(clientSessionSingleResultCallback -> wrapped.startSession(options,
                new SingleResultCallback<AsyncClientSession>() {
                    @Override
                    public void onResult(final AsyncClientSession result, final Throwable t) {
                        if (t != null) {
                            clientSessionSingleResultCallback.onResult(null, t);
                        } else {
                            clientSessionSingleResultCallback.onResult(new ClientSessionImpl(result, this), null);
                        }
                    }
                }));
    }

    @Override
    public ClusterDescription getClusterDescription() {
        return wrapped.getClusterDescription();
    }
}
