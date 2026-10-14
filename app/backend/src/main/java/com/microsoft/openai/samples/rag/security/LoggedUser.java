package com.microsoft.openai.samples.rag.security;

public record  LoggedUser(String username, String mail, String role, String displayName, String entraId) {

}
