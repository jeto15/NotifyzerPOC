package com.example.notifyzerpocphase1.util

import com.example.notifyzerpocphase1.data.EntityProfile
import com.example.notifyzerpocphase1.model.CapturedNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DossierPromptBuilder {

    fun buildPrompt(
        displayName: String,
        groupLogs: List<CapturedNotification>,
        entityProfile: EntityProfile? = null
    ): String {
        val identityRole = entityProfile?.identityRole ?: "User seeking strategic relationship & communication guidance"
        val historyContext = entityProfile?.history ?: "Ongoing message history with contact $displayName"
        val currentDynamic = entityProfile?.currentDynamic ?: "Active message exchange requiring strategic analysis and response planning"
        val tacticalObjective = entityProfile?.tacticalObjective ?: "Maintain emotional centering, enforce healthy boundaries, avoid over-pursuing, and guide communication effectively"
        val sortedLogs = groupLogs.sortedBy { it.timestamp }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        val latestMessage = sortedLogs.lastOrNull()
        val historicalMessages = if (sortedLogs.size > 1) sortedLogs.dropLast(1) else emptyList()

        val historicalFormattedRecords = if (historicalMessages.isNotEmpty()) {
            historicalMessages.joinToString("\n") { log ->
                val timeStr = dateFormat.format(Date(log.timestamp))
                val isSent = log.packageName == "historical.sms.sync.sent" || log.title == "Me"
                val direction = if (isSent) "Outgoing" else "Incoming"
                "$timeStr | [$direction] | ${log.text ?: "(No Text)"}"
            }
        } else {
            "(No prior historical messages captured)"
        }

        val latestTimestamp = latestMessage?.timestamp?.let { dateFormat.format(Date(it)) } ?: "N/A"
        val latestContent = latestMessage?.text ?: "(No Content)"

        return """
You are an elite communication and relationship dynamics coach. Your analytical framework strictly combines the methodologies of Corey Wayne (focusing on emotional centering, the "tennis match" of communication, matching effort, and avoiding over-pursuing) and Marcus Taylor (focusing on extreme accountability, emotional intelligence, setting firm boundaries, and breaking toxic patterns). 

You are analyzing an ongoing text message thread. Your goal is to generate a tactical coaching dossier based on the user's defined objectives, the historical message context, and the single newest message received.

### PART 1: RULES OF ENGAGEMENT
Evaluate all communication through the lens of the user's Initial Context Protocol:
* Identity & Role: $identityRole
* Historical Context: $historyContext
* Current Dynamic: $currentDynamic
* Tactical Objective: $tacticalObjective

### PART 2: THE COMMUNICATION TIMELINE
Below is the historical communication record for context, followed by the latest message that requires immediate analysis. 

$historicalFormattedRecords

--- LATEST MESSAGE INTERCEPTED ---
Date/Time: $latestTimestamp
Sender: $displayName
Message: $latestContent

### PART 3: DOSSIER GENERATION REQUIREMENTS
Generate a concise, highly direct coaching dossier based on the Corey Wayne and Marcus Taylor frameworks. Do not flatter the user; hold them accountable to their Tactical Objective.

Format your response using the following structure:

1. The Subtext Analysis
Analyze the exact subtext of the "LATEST MESSAGE INTERCEPTED". Is it a test of boundaries? Is it high-interest or low-effort? Are they trying to pull the user off-center?

2. Power Dynamic & Pacing Check (Corey Wayne Framework)
Evaluate the "tennis match". Based on the historical context and the latest message, who is over-pursuing? Is the user matching effort, or are they falling into the "illusion of action" (doing too much)? Advise on the correct pacing and emotional centering required right now.

3. Tactical Accountability (Marcus Taylor Framework)
Evaluate the user's position against their stated "Tactical Objective". Are they upholding their boundaries? Give a direct, no-nonsense directive on how to handle this specific message to achieve their objective (e.g., "Do not respond for 24 hours," "Mirror their brevity," "Enforce the boundary with this specific phrasing").
""".trimIndent()
    }
}
