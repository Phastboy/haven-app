package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Requests a Google ID token through the system Credential Manager.
 *
 * The returned token is verified by Haven's backend; this class never trusts or
 * parses the identity claims itself.
 */
object NativeGoogleSignIn {
  suspend fun idToken(context: Context, serverClientId: String, nonce: String?): String {
    val googleIdOption = GetGoogleIdOption.Builder()
      .setServerClientId(serverClientId)
      .setFilterByAuthorizedAccounts(false)
      .setAutoSelectEnabled(false)
      .apply { if (nonce != null) setNonce(nonce) }
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    val credential = CredentialManager.create(context)
      .getCredential(context, request)
      .credential
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    throw IllegalStateException("Google did not return an ID token.")
  }
}
