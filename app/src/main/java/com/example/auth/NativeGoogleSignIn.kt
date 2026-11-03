package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Requests a Google ID token through the system Credential Manager.
 *
 * The returned token is verified by Haven's backend; this class never trusts or
 * parses the identity claims itself.
 */
object NativeGoogleSignIn {
  suspend fun idToken(context: Context, serverClientId: String, nonce: String?): String {
    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "CONFIGURING_REQUEST",
      message = "Configuring GetGoogleIdOption & GetSignInWithGoogleOption with serverClientId=$serverClientId, autoSelectEnabled=false, filterByAuthorizedAccounts=false, hasNonce=${nonce != null}"
    )
    val googleIdOption = GetGoogleIdOption.Builder()
      .setServerClientId(serverClientId)
      .setFilterByAuthorizedAccounts(false)
      .setAutoSelectEnabled(false)
      .apply { if (nonce != null) setNonce(nonce) }
      .build()
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
      .apply { if (nonce != null) setNonce(nonce) }
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .addCredentialOption(signInWithGoogleOption)
      .build()

    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "DISPATCHING_GET_CREDENTIAL",
      message = "Calling CredentialManager.create(context).getCredential(...) to display native Google One-Tap / Account sheet"
    )
    val credential = try {
      CredentialManager.create(context)
        .getCredential(context, request)
        .credential
    } catch (e: Exception) {
      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "GET_CREDENTIAL_THREW_EXCEPTION",
        message = "CredentialManager.getCredential failed with exception ${e.javaClass.name}: ${e.message}",
        isError = true,
        throwable = e
      )
      throw e
    }

    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "CREDENTIAL_RESULT_RECEIVED",
      message = "Credential returned: class=${credential.javaClass.name}, type=${(credential as? CustomCredential)?.type ?: "non-custom"}"
    )

    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "ID_TOKEN_EXTRACTED",
        message = "Successfully extracted Google ID token (length=${token.length}, prefix=${token.take(12)}...)"
      )
      return token
    }
    val failureMsg = "Google did not return an ID token. Credential type=${credential.type}."
    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "UNEXPECTED_CREDENTIAL_TYPE",
      message = failureMsg,
      isError = true
    )
    throw IllegalStateException(failureMsg)
  }
}
