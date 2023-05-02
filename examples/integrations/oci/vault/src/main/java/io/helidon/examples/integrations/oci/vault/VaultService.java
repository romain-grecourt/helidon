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

package io.helidon.examples.integrations.oci.vault;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.common.Base64Value;

import com.oracle.bmc.keymanagement.KmsCrypto;
import com.oracle.bmc.keymanagement.responses.DecryptResponse;
import com.oracle.bmc.keymanagement.responses.EncryptResponse;
import com.oracle.bmc.keymanagement.responses.SignResponse;
import com.oracle.bmc.keymanagement.responses.VerifyResponse;
import com.oracle.bmc.secrets.Secrets;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import com.oracle.bmc.vault.Vaults;
import com.oracle.bmc.vault.responses.CreateSecretResponse;
import com.oracle.bmc.vault.responses.ScheduleSecretDeletionResponse;

import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails;
import com.oracle.bmc.keymanagement.model.SignDataDetails;
import com.oracle.bmc.keymanagement.model.VerifyDataDetails;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.requests.EncryptRequest;
import com.oracle.bmc.keymanagement.requests.SignRequest;
import com.oracle.bmc.keymanagement.requests.VerifyRequest;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.vault.model.Base64SecretContentDetails;
import com.oracle.bmc.vault.model.CreateSecretDetails;
import com.oracle.bmc.vault.model.ScheduleSecretDeletionDetails;
import com.oracle.bmc.vault.model.SecretContentDetails;
import com.oracle.bmc.vault.requests.CreateSecretRequest;
import com.oracle.bmc.vault.requests.ScheduleSecretDeletionRequest;

class VaultService implements HttpService {
    private final Secrets secrets;
    private final Vaults vaults;
    private final KmsCrypto crypto;
    private final String vaultOcid;
    private final String compartmentOcid;
    private final String encryptionKeyOcid;
    private final String signatureKeyOcid;

    VaultService(Secrets secrets,
                 Vaults vaults,
                 KmsCrypto crypto,
                 String vaultOcid,
                 String compartmentOcid,
                 String encryptionKeyOcid,
                 String signatureKeyOcid) {
        this.secrets = secrets;
        this.vaults = vaults;
        this.crypto = crypto;
        this.vaultOcid = vaultOcid;
        this.compartmentOcid = compartmentOcid;
        this.encryptionKeyOcid = encryptionKeyOcid;
        this.signatureKeyOcid = signatureKeyOcid;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/encrypt/{text:.*}", this::encrypt)
             .get("/decrypt/{text:.*}", this::decrypt)
             .get("/sign/{text}", this::sign)
             .post("/verify/{text}", this::verify)
             .get("/secret/{id}", this::getSecret)
             .post("/secret/{name}", this::createSecret)
             .delete("/secret/{id}", this::deleteSecret);
    }

    private void getSecret(ServerRequest req, ServerResponse res) {
        GetSecretBundleResponse secretsResponse = secrets.getSecretBundle(
                GetSecretBundleRequest.builder()
                                      .secretId(req.path().pathParameters().value("id"))
                                      .build());

        SecretBundleContentDetails content = secretsResponse.getSecretBundle().getSecretBundleContent();
        if (content instanceof Base64SecretBundleContentDetails details) {
            // the only supported type
            res.send(Base64Value.createFromEncoded(details.getContent()).toDecodedString());
        } else {
            throw new IllegalStateException("Invalid secret content type");
        }
    }

    private void deleteSecret(ServerRequest req, ServerResponse res) {
        // has to be for quite a long period of time - did not work with less than 30 days
        Date deleteTime = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

        String secretOcid = req.path().pathParameters().value("id");

        ScheduleSecretDeletionResponse vaultsResponse = vaults.scheduleSecretDeletion(
                ScheduleSecretDeletionRequest.builder()
                                             .secretId(secretOcid)
                                             .scheduleSecretDeletionDetails(
                                                     ScheduleSecretDeletionDetails.builder()
                                                                                  .timeOfDeletion(deleteTime)
                                                                                  .build())
                                             .build());

        res.send("Secret " + vaultsResponse + " was marked for deletion");
    }

    private void createSecret(ServerRequest req, ServerResponse res) {
        String secretName = req.path().pathParameters().value("name");
        String secretText = req.content().as(String.class);
        SecretContentDetails content = Base64SecretContentDetails.builder()
                                                                 .content(Base64Value.create(secretText).toBase64())
                                                                 .build();

        CreateSecretResponse vaultsResponse = vaults.createSecret(
                CreateSecretRequest.builder()
                                   .createSecretDetails(CreateSecretDetails.builder()
                                                                           .secretName(secretName)
                                                                           .vaultId(vaultOcid)
                                                                           .compartmentId(compartmentOcid)
                                                                           .keyId(encryptionKeyOcid)
                                                                           .secretContent(content)
                                                                           .build())
                                   .build());

        res.send(vaultsResponse.getSecret().getId());
    }

    private void verify(ServerRequest req, ServerResponse res) {
        String text = req.path().pathParameters().value("text");
        String signature = req.content().as(String.class);
        VerifyDataDetails.SigningAlgorithm algorithm = VerifyDataDetails.SigningAlgorithm.Sha224RsaPkcsPss;

        VerifyResponse verifyResponse = crypto.verify(VerifyRequest.builder()
                                                                   .verifyDataDetails(VerifyDataDetails.builder()
                                                                                                       .keyId(signatureKeyOcid)
                                                                                                       .signingAlgorithm(algorithm)
                                                                                                       .message(Base64Value.create(text).toBase64())
                                                                                                       .signature(signature)
                                                                                                       .build())
                                                                   .build());
        boolean valid = verifyResponse.getVerifiedData()
                                      .getIsSignatureValid();
        res.send(valid ? "Signature valid" : "Signature not valid");
    }

    private void sign(ServerRequest req, ServerResponse res) {
        String text = req.path().pathParameters().value("text");
        SignResponse cryptoResponse = crypto.sign(
                SignRequest.builder()
                           .signDataDetails(
                                   SignDataDetails.builder()
                                                  .keyId(signatureKeyOcid)
                                                  .signingAlgorithm(SignDataDetails.SigningAlgorithm.Sha224RsaPkcsPss)
                                                  .message(Base64Value.create(text).toBase64())
                                                  .build())
                           .build());

        res.send(cryptoResponse.getSignedData().getSignature());
    }

    private void encrypt(ServerRequest req, ServerResponse res) {
        String text = req.path().pathParameters().value("text");
        EncryptResponse cryptoResponse = crypto.encrypt(
                EncryptRequest.builder()
                              .encryptDataDetails(
                                      EncryptDataDetails.builder()
                                                        .keyId(encryptionKeyOcid)
                                                        .plaintext(Base64Value.create(text).toBase64())
                                                        .build())
                              .build());

        res.send(cryptoResponse.getEncryptedData().getCiphertext());
    }

    private void decrypt(ServerRequest req, ServerResponse res) {
        String text = req.path().pathParameters().value("text");
        DecryptResponse cryptoResponse = crypto.decrypt(
                DecryptRequest.builder()
                              .decryptDataDetails(
                                      DecryptDataDetails.builder()
                                                        .keyId(encryptionKeyOcid)
                                                        .ciphertext(text)
                                                        .build())
                              .build());

        res.send(Base64Value.createFromEncoded(cryptoResponse.getDecryptedData()
                                                             .getPlaintext())
                            .toDecodedString());
    }
}
