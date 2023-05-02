/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.integrations.oci.objecstorage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.RenameObjectDetails;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.requests.RenameObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;

class ObjectStorageService implements HttpService {
    private static final Logger LOGGER = Logger.getLogger(ObjectStorageService.class.getName());
    private final ObjectStorage client;
    private final String bucketName;
    private final String namespaceName;

    ObjectStorageService(ObjectStorage client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
        this.namespaceName = this.client.getNamespace(GetNamespaceRequest.builder().build())
                                        .getValue();
    }


    @Override
    public void routing(HttpRules rules) {
        rules.get("/file/{file-name}", this::download)
             .post("/file/{file-name}", this::upload)
             .delete("/file/{file-name}", this::delete)
             .get("/rename/{old-name}/{new-name}", this::rename);
    }

    private void delete(ServerRequest req, ServerResponse res) {
        String objectName = req.path().pathParameters().value("file-name");

        client.deleteObject(DeleteObjectRequest.builder()
                                               .namespaceName(namespaceName)
                                               .bucketName(bucketName)
                                               .objectName(objectName).build());
        res.status(Http.Status.OK_200);
    }

    private void rename(ServerRequest req, ServerResponse res) {
        String oldName = req.path().pathParameters().value("old-name");
        String newName = req.path().pathParameters().value("new-name");

        client.renameObject(
                RenameObjectRequest.builder()
                                   .namespaceName(namespaceName)
                                   .bucketName(bucketName)
                                   .renameObjectDetails(
                                           RenameObjectDetails.builder()
                                                              .newName(newName)
                                                              .sourceName(oldName)
                                                              .build())
                                   .build());
        res.status(Http.Status.OK_200).send();
    }

    private void upload(ServerRequest req, ServerResponse res) {
        String objectName = req.path().pathParameters().value("file-name");
        String filePath = System.getProperty("user.dir") + File.separator + objectName;
        try (InputStream stream = new FileInputStream(filePath)) {
            byte[] contents = stream.readAllBytes();
            client.putObject(PutObjectRequest.builder()
                                             .namespaceName(namespaceName)
                                             .bucketName(bucketName)
                                             .objectName(objectName)
                                             .putObjectBody(new ByteArrayInputStream(contents))
                                             .contentLength((long) contents.length)
                                             .build());
            res.status(Http.Status.OK_200).send();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void download(ServerRequest req, ServerResponse res) {
        String objectName = req.path().pathParameters().value("file-name");
        GetObjectResponse getObjectResponse = client.getObject(
                GetObjectRequest.builder()
                                .namespaceName(namespaceName)
                                .bucketName(bucketName)
                                .objectName(objectName)
                                .build());

        if (getObjectResponse.getContentLength() == 0) {
            LOGGER.log(Level.SEVERE, "GetObjectResponse is empty");
            res.status(Http.Status.NOT_FOUND_404).send();
            return;
        }

        try (InputStream fileStream = getObjectResponse.getInputStream()) {
            byte[] objectContent = fileStream.readAllBytes();
            res.header(Http.Header.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
               .status(Http.Status.OK_200)
               .send(objectContent);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
