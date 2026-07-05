package com.example.sfmcregister

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.pushfeature.PushFeature
import com.salesforce.marketingcloud.pushfeature.push.PushMessageManager

/**
 * FirebaseMessagingService — meneruskan push token & pesan MC ke SDK.
 * Mengikuti pola MyFcmMessagingService di LearningApp resmi Salesforce.
 */
class SfmcFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        // Hanya pesan dari Marketing Cloud yang diteruskan ke SDK.
        if (PushMessageManager.isMarketingCloudPush(message)) {
            PushFeature.requestSdk { pushFeature ->
                pushFeature.getPushMessageManager().handleMessage(message)
            }
        }
        // Pesan non-MC: tangani sendiri bila perlu.
    }

    override fun onNewToken(token: String) {
        // Token baru wajib diserahkan ke SDK → memicu registrasi ter-update ke MC.
        PushFeature.requestSdk { pushFeature ->
            pushFeature.getPushMessageManager().setPushToken(token)
        }

    }
}
