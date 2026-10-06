# Next Sprint Plan: Monetization & Initial Context Protocol

This document outlines the architecture and implementation steps for the upcoming sprint, based on Ted's requirements for dual-tier monetization and the AI onboarding context protocol.

---

## Part 1: Initial Context Protocol (EntityProfile)

**Goal:** Intercept the AI Dossier generation to gather baseline relationship context from the user, ensuring the AI scores the interaction based on defined tactical objectives.

### Implementation Steps:
1.  **Database Updates (`AppDatabase` / `EntityProfileDao`)**
    *   Create an `EntityProfile` Room entity to store contact-specific context.
    *   Fields required: `phoneNumber` (Primary Key), `identityRole`, `history`, `currentDynamic`, `tacticalObjective`.
2.  **The Intercept Trigger (`MainScreen.kt` / `MainViewModel.kt`)**
    *   When the user taps the `★ ✨ Summary` button, check if an `EntityProfile` exists for that `senderKey`.
    *   If `null`, pause the dossier generation and trigger the Onboarding UI.
3.  **The AI Onboarding Prompt UI**
    *   Build a new Composable Modal/Dialog or Bottom Sheet with 4 input fields:
        *   **Identity & Role:** Who is this person to you?
        *   **History:** How long have you known them / relevant historical context?
        *   **Dynamic:** What is the current nature of the relationship?
        *   **Tactical Objective:** What defines a "successful" communication loop here?
4.  **Database Storage & Injection (`DossierPromptBuilder.kt`)**
    *   Save the user's answers to the `EntityProfile` in Room DB.
    *   Update `generateDossierForContact` to fetch this profile.
    *   Inject the saved answers into the `[INJECT_...]` placeholders in `DossierPromptBuilder`.
    *   Resume the dossier generation call.

---

## Part 2: Dual-Tier Monetization & Metering System

**Goal:** Support both a BYOK (Bring Your Own Key) tier and a Fully Integrated Premium Tier ($30/mo) with a token metering system and top-up UX flow.

### Implementation Steps:
1.  **Database Updates (`UserCreditsEntity`)**
    *   Create a local token/credit tracking mechanism in Room DB (precursor to remote sync).
    *   Track `isPremiumTier`, `monthlyAllowanceBalance`, and `currentCycleEnd`.
2.  **Key-Switching Logic (`GeminiRepository.kt` / `MainViewModel.kt`)**
    *   Architect the router:
        *   If `isPremiumTier == false`, route through the existing `ApiKeyManager` (BYOK).
        *   If `isPremiumTier == true`, bypass BYOK and use the encrypted Master API Key.
3.  **Internal Metering System**
    *   For Premium users, check `monthlyAllowanceBalance` before calling the Gemini API.
    *   If `balance > 0`, decrement the balance by 1 upon successful dossier generation.
4.  **UX Flow for Credit Top-Ups**
    *   If a Premium user taps `★ ✨ Summary` and `monthlyAllowanceBalance == 0`:
    *   Intercept the call and display an "Out of Credits" Bottom Sheet.
    *   Prompt users to purchase additional credit top-ups ($5, $10, or $15 tiers).
