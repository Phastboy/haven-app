package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Requests a Google ID token through the explicit Google sign-in button.
 *
 * The returned token must be verified by Haven's backend.
 */
object NativeGoogleSignIn {

  suspend fun idToken(
    context: Context,
    serverClientId: String,
    nonce: String?
  ): String {

    require(serverClientId.isNotBlank()) {
      "Google Web Client ID must not be empty."
    }

    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "CONFIGURING_REQUEST",
      message = "Configuring explicit Google sign-in; hasNonce=${nonce != null}"
    )

    val googleOption =
      GetSignInWithGoogleOption.Builder(serverClientId)
        .apply {
          if (nonce != null) setNonce(nonce)
        }
        .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleOption)
      .build()

    val credential = try {
      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "DISPATCHING_GET_CREDENTIAL",
        message = "Requesting explicit Google sign-in"
      )

      CredentialManager.create(context)
        .getCredential(
          context = context,
          request = request
        )
        .credential

    } catch (e: GetCredentialCancellationException) {
      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "SIGN_IN_CANCELLED",
        message = "User cancelled Google sign-in",
        throwable = e
      )
      throw e

    } catch (e: Exception) {
      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "GET_CREDENTIAL_FAILED",
        message = "Exception: ${e.javaClass.name}; message=${e.message}",
        isError = true,
        throwable = e
      )
      throw e
    }

    if (
      credential !is CustomCredential ||
      credential.type !=
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val message =
        "Unexpected credential type: ${credential.type}"

      HavenAuthClient.logDiagnostic(
        flow = "NATIVE_CREDENTIAL_MANAGER",
        step = "UNEXPECTED_CREDENTIAL",
        message = message,
        isError = true
      )

      throw IllegalStateException(message)
    }

    val token = GoogleIdTokenCredential
      .createFrom(credential.data)
      .idToken

    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_CREDENTIAL_MANAGER",
      step = "ID_TOKEN_EXTRACTED",
      message = "Google ID token successfully extracted"
    )

    return token
  }
}
