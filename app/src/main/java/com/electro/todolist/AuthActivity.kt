/*
 * Copyright (c) 2021. Electro Inc.
 * This project and all its code belong to and were created by Evan Cocain, as know as Electro Inc.
 * You are not allowed to modify, share or re-use this.
 */
@file:Suppress("unused")

package com.electro.todolist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest

import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.electro.todolist.BuildConfig
import com.electro.todolist.R
import timber.log.Timber
import java.lang.reflect.Type


// This activity manages the authentication : connexion to an account or not
class AuthActivity : AppCompatActivity() {
  /*  private lateinit var mAuth: FirebaseAuth
    private lateinit var dataSaved: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_loading_auth)

        dataSaved = getSharedPreferences("dataSaved", 0)

        mAuth = Firebase.auth
        val isUserNull = mAuth.currentUser == null
        val needsConnection = dataSaved.getBoolean("connexion", true)

        val convertToPlayGames = intent.getBooleanExtra("convertToPlayGames", false)

        if (convertToPlayGames) convertAccountToPlayGames()
        else if (isUserNull || needsConnection) startAuth()
        else startMainActivity(saveOnCreate = false, false)
    }

    private fun startAuth(forceGooglePlay: Boolean = false) {
        val requestCode = if (forceGooglePlay) 321 else 123

        val providers = if (forceGooglePlay) {
            listOf(
                GoogleBuilder().setSignInOptions(
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .build() //.requestScopes(Drive.SCOPE_APPFOLDER)
                ).build(),
            )
        } else {
            listOf(
                GoogleBuilder().setSignInOptions(
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .build() //.requestScopes(Drive.SCOPE_APPFOLDER)
                ).build(),
                AnonymousBuilder().build(), // REMOVED BECAUSE OF UNUSED ACCOUNTS
                //PhoneBuilder().build(), // REMOVED BECAUSE OF UNUSED COSTS
                EmailBuilder().build()
            )
        }

        val customLayout = if (forceGooglePlay) {
            AuthMethodPickerLayout.Builder(R.layout.activity_auth_with_gp)
                .setGoogleButtonId(R.id.with_google)
                .setTosAndPrivacyPolicyId(R.id.legal_infos)
                .build()
        } else {
            AuthMethodPickerLayout.Builder(R.layout.activity_auth)
                .setGoogleButtonId(R.id.with_google)
                .setEmailButtonId(R.id.with_mail)
                .setAnonymousButtonId(R.id.no_connexion)
                .setTosAndPrivacyPolicyId(R.id.legal_infos)
                .build()
        }

        //
// .setAlwaysShowSignInMethodScreen(false)
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()

                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLayout)
              *//*  .setDefaultProvider(
                    GoogleBuilder().setSignInOptions(
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .build() //.requestScopes(Drive.SCOPE_APPFOLDER)
                    ).build()
                )*//*
                .setTheme(R.style.AuthThemeMaterial)
                //.setTimbero(R.drawable.maths3)
                .setTosAndPrivacyPolicyUrls(
                    "https://docs.google.com/document/d/1TtTjZ5JhBshqmwSwT9wVBC6i-SSybt5QZqIGifs_Yk4/",
                    "https://docs.google.com/document/d/1TtTjZ5JhBshqmwSwT9wVBC6i-SSybt5QZqIGifs_Yk4/"
                )
                .setIsSmartLockEnabled(false, false)
                .build(),
            requestCode
        )
        Timber.i("FirebaseUI Authentication Flow started")
    }

    private fun restoreDataThenStart(usr: FirebaseUser) {

        val firstLaunch = dataSaved.getBoolean("firstLaunch", true)

        val gemsHelper = GemsHelper(this)

        val userPath = usr.let {
            Firebase.database.reference.child("users").child(it.uid)
        }

        val userProvider = usr.providerData

        Timber.i("User Provider data : $userProvider")
        // IF ANONYME >> NO SYNC

        userProvider.forEach { userInfo ->
            userInfo?.let {
                Timber.i("User Provider : ${userInfo.providerId}, Specfic UID : ${userInfo.uid}")
            }
        }

        val lowestProvider = userProvider[userProvider.size - 1]
        Timber.i("Data Provider ID last : ${lowestProvider.providerId}")

        Timber.i("Sync : Updating Local data from Database")

        // Get Rewards
        // Get Scores
        // Get Stats

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            Toast.makeText(this, "L'opération prend plus de temps que prévu, veuillez patienter", Toast.LENGTH_SHORT).show()}, 3000)

        handler.postDelayed({startMainActivity(saveOnCreate = false, firstLaunch) }, 8000)

        userPath.get().addOnCompleteListener { snapshot ->
            handler.removeCallbacksAndMessages(null)

            snapshot.result?.let { result ->
                with(result) {
                    val editor = dataSaved.edit()

                    child("xp").getValue<Int>()?.let { NnbXp ->
                        Timber.e("GOT XP VALUE")
                        editor.putInt("xp", NnbXp)
                    } ?: run {
                        Timber.e("XP : cloud value is NULL, XP must be reset")
                    }

                    child("gems").getValue<Int>()?.let { NnbGems ->
                        Timber.e("GEMS GOT : $NnbGems")

                        editor.putInt("saphirs", NnbGems)
                        gemsHelper.resetGemsVariation()
                    } ?: run {
                        Timber.e("GEMS : cloud value is NULL, GEMS must be reset")
                    }

                    child("streaks").getValue<Int>()?.let { NnbStreaks ->

                        Timber.e("Streaks Cloud value is not null, updating local, New value : $NnbStreaks")

                        editor.putInt("dayStreak", NnbStreaks)


                    } ?: run {
                        Timber.e("Streaks : cloud value is NULL, Streaks must be reset")
                    }

                    Timber.e("STREAKS VALUE FROM DB INIT : ${StreaksHelper(this@AuthActivity).getCurrentStreaksCount()}")

                    child("achievements").getValue<String>()?.let { jsonString ->
                        Timber.e("GOT ACHIEVEMENTS VALUE")
                        try {
                            Timber.e("achievements Cloud : $jsonString")

                            val listType: Type = object : TypeToken<HashMap<String, Boolean>>() {}.type
                            val hashMap: HashMap<String, Boolean> =
                                Gson().fromJson(jsonString, listType)


                            Timber.e("Result")

                            Timber.e(Gson().toJson(hashMap))
                            //Timber.e(Json.encodeToString(hashMap))

                            editor.apply {
                                hashMap.toMutableMap().forEach {
                                    putBoolean(it.key, it.value)
                                    Timber.e("REWARD SET FROM DB")
                                }
                            }

                        } catch (e: Exception) {
                            //Toast.makeText(this@AuthActivity, "Error decoding Rewards Map", Toast.LENGTH_SHORT).show()
                            Toast.makeText(this@AuthActivity, e.localizedMessage, Toast.LENGTH_LONG)
                                .show()
                            if (BuildConfig.DEBUG) Toast.makeText(
                                this@AuthActivity, getString(
                                    R.string.error_achievements
                                ), Toast.LENGTH_LONG
                            ).show()
                            Timber.e(e)
                        }
                    } ?: run {
                        Timber.e("ACHIEVEMENTS : cloud value is NULL, ACHIEVEMENTS must be reset")
                    }

                    child("gameHistory").getValue<String>()?.let { jsonString ->
                        Timber.e("GOT gameHistory VALUE")
                        try {
                            Timber.e("gameHistory Cloud : $jsonString")

                            if (jsonString.isNotBlank()) {
                                val listType: Type =
                                    object : TypeToken<ArrayList<GameHistory>>() {}.type
                                val currentHistory: java.util.ArrayList<GameHistory> = Gson().fromJson(
                                    jsonString, listType
                                ) //Json.decodeFromString(jsonString)
                                if (!currentHistory.isEmpty()) {
                                    val updatedJson =
                                        Gson().toJson(currentHistory) //Json.encodeToString(currentHistory)
                                    editor.putString("gameHistory", updatedJson)
                                }
                            }
                        } catch (e: Exception) {
                            if (BuildConfig.DEBUG) Toast.makeText(
                                this@AuthActivity,
                                e.localizedMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            if (BuildConfig.DEBUG) Toast.makeText(
                                this@AuthActivity, getString(
                                    R.string.error_achievements
                                ), Toast.LENGTH_LONG
                            ).show()
                            Timber.e(e)
                        }
                    } ?: run {
                        Timber.e("ACHIEVEMENTS : cloud value is NULL, ACHIEVEMENTS must be reset")
                    }

                    child("items").apply {
                        val itemsHelper = ItemsHelper(this@AuthActivity)

                        itemsHelper.allItems.forEach { item ->
                            child(item.id).getValue<Int>()?.let {
                                editor.putInt(item.id, it)

                                Timber.e("Sync Item ${item.id}")
                            }
                        }
                    }

                    child("stats").apply {
                        child("rightAnswers").getValue<Int>()?.let {
                            editor.putInt("rightAnswers", it)

                            Timber.e("Sync Stats 1/4")
                        }

                        child("wrongAnswers").getValue<Int>()?.let {
                            editor.putInt("wrongAnswers", it)

                            Timber.e("Sync Stats 2/4")
                        }

                        child("gamesPlayed").getValue<Int>()?.let {
                            editor.putInt("gamesPlayed", it)

                            Timber.e("Sync Stats 3/4")
                        }

                        child("lastGame").getValue<Long>()?.let {
                            editor.putLong("lastGame", it)

                            Timber.e("Sync Stats 4/4")
                        }
                    }

                    child("settings").apply {
                        child("autoValidation").getValue<Boolean>()?.let {
                            editor.putBoolean("auto_validationBool", it)

                            Timber.e("Sync Settings 1/7")
                        }

                        child("soundEnabled").getValue<Boolean>()?.let {
                            editor.putBoolean("sound_onBool", it)

                            Timber.e("Sync Settings 2/7")
                        }

                        child("vibrateEnabled").getValue<Boolean>()?.let {
                            editor.putBoolean("vibrate_onBool", it)

                            Timber.e("Sync Settings 3/7")
                        }

                        child("flagExperimental").getValue<Boolean>()?.let {
                            editor.putBoolean("flag_experimental", it)

                            Timber.e("Sync Settings 4/7")
                        }
                        child("newNavBar").getValue<Boolean>()?.let {
                            editor.putBoolean("new_exp_nav_bar", it)

                            Timber.e("Sync Settings 5/7")
                        }
                        child("expLoadingScreen").getValue<Boolean>()?.let {
                            editor.putBoolean("expLoadingScreen", it)

                            Timber.e("Sync Settings 6/7")
                        }
                        child("notifs").apply {
                            child("notify").getValue<Boolean>()?.let {
                                editor.putBoolean("notify_bool", it)

                                Timber.e("Sync Notifs 4/7")
                            }
                            child("hourNotif").getValue<Int>()?.let {
                                editor.putInt("hourNotif", it)

                                Timber.e("Sync Notifs 5/7")
                            }
                            child("minutesNotif").getValue<Int>()?.let {
                                editor.putInt("minutesNotif", it)

                                Timber.e("Sync Notifs 6/7")
                            }
                            child("interval").getValue<String>()?.let {
                                editor.putString("stringInterval", it)

                                Timber.e("Sync Notifs 6/7")
                            }
                        }
                    }
                    editor.apply()
                }
            }


            startMainActivity(saveOnCreate = false, firstLaunch)
        }
        Timber.e("INITIAL SYNC Done successfully")
    }

    @SuppressLint("HardwareIds")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val firstLaunch = dataSaved.getBoolean("firstLaunch", true)

        fun changeSignedDevice(usr: FirebaseUser) {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

            val childUpdates = hashMapOf<String, Any>()

            val database = Firebase.database.reference // We get the reference to the database
            val userPath = database.child("users").child(usr.uid) // We get the path to the user


            childUpdates["signedDevice"] = androidId

            userPath.updateChildren(childUpdates).addOnSuccessListener {
                *//*if (BuildConfig.DEBUG) Toast.makeText(
                    this,
                    "CHANGED SIGNED DEVICE",
                    Toast.LENGTH_SHORT
                ).show()*//*
                Timber.e("Synced SIGNED DEVICE to Database successfully")
            }.addOnFailureListener {
                Timber.e("Offline, cannot sync SIGNED DEVICE")
            }

        }

        fun updateName(name: String?, usr: FirebaseUser) {

            //Toast.makeText(this, "Name updateDB is $name", Toast.LENGTH_SHORT).show()
            val childUpdates = hashMapOf<String, Any>()

            val database = Firebase.database.reference // We get the reference to the database
            val userPath = database.child("users").child(usr.uid) // We get the path to the user

            name?.let {
                if (name.isNotBlank()) childUpdates["name"] = name

                userPath.updateChildren(childUpdates).addOnSuccessListener {
                    Timber.e("Synced SIGNED DEVICE to Database successfully")
                }.addOnFailureListener {
                    Timber.e("Offline, cannot sync SIGNED DEVICE")
                }
            }
        }



        // * CONVERT TO GP

        fun firebaseAuthUpdateDisplayName(newDisplayName : String, user: FirebaseUser) {
            val profileUpdates = userProfileChangeRequest {
                displayName = newDisplayName
                //  photoUri = Uri.parse("https://example.com/jane-q-user/profile.jpg")
            }

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (BuildConfig.DEBUG) Toast.makeText(
                            this,
                            "Updated FIREBASE name to $newDisplayName successfull",
                            Toast.LENGTH_SHORT
                        ).show()

                        Timber.e("User profile updated.")
                    }
                }
        }

        fun checkNameReplace(user : FirebaseUser) {
            val firebaseName = user.displayName

            GoogleSignIn.getLastSignedInAccount(this)?.let { account ->
                val clientP = Games.getPlayersClient(this, account)

                clientP.currentPlayer.addOnCompleteListener {
                    if (it.isSuccessful) {
                        *//*if (BuildConfig.DEBUG) Toast.makeText(
                            this,
                            "RUNNING NAME CHECK",
                            Toast.LENGTH_SHORT
                        ).show()*//*

                        val playGamesName = it.result?.displayName ?: ""

                        if (playGamesName.isNotBlank() && playGamesName != firebaseName) {
                            updateName(playGamesName, user)

                            firebaseAuthUpdateDisplayName(playGamesName, user)
                        } else updateName(firebaseName, user)

                    } else updateName(firebaseName, user)
                }
            } ?: updateName(firebaseName, user)
        }

        fun processSignedAccount(codeForResult : Int, data : Intent?, normalAuth : Boolean) {

            Timber.i("Request Code from AuthUI received, processing check-in...")
            val response = IdpResponse.fromResultIntent(data)

            if (codeForResult == RESULT_OK) { // Successfully signed in

                if (normalAuth) dataSaved.edit().remove("saphirs").apply()

                mAuth.currentUser?.let { usr ->
                    changeSignedDevice(usr)

                    checkNameReplace(usr)

                    if (normalAuth) restoreDataThenStart(usr)
                    else startMainActivity(saveOnCreate = !normalAuth, firstLaunch)

                } ?: startMainActivity(saveOnCreate = !normalAuth, !normalAuth)

                // ? SIGNED IN, WE SET THE ANDROID ID

            } else { // Sign in failed
                dataSaved.edit().putBoolean("connexion", false).apply()
                when {
                    response == null -> { // User pressed back button
                        Toast.makeText(this, getS(this, R.string.auth_aborted), Toast.LENGTH_SHORT)
                            .show()
                    }
                    response.error!!.errorCode == ErrorCodes.NO_NETWORK -> {
                        Toast.makeText(
                            this,
                            getS(this, R.string.no_network_access),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    else -> {
                        Toast.makeText(
                            this,
                            getS(this, R.string.you_can_sign_in_later),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        Timber.e("Error ${response.error?.errorCode}")
                    }
                }
                startMainActivity(saveOnCreate = false, firstLaunch)
            }
        }

        if (requestCode == 321) {
            processSignedAccount(resultCode, data, false)
        }

        if (requestCode == 123) {
            processSignedAccount(resultCode, data, true)
        }
    }

    *//*private fun firebaseAuthWithPlayGames(serverCredential : String) {
        Timber.e("firebaseAuthWithPlayGames")

        val auth = Firebase.auth
        val credential = PlayGamesAuthProvider.getCredential(serverCredential)
        // TODO : THE ISSUE HAPPENS HERE...
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.e("signInWithCredential:success")

                    //Toast.makeText(this, "Task successfull", Toast.LENGTH_SHORT).show()

                    dataSaved.edit().remove("saphirs").apply()

                    val android_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

                    auth.currentUser?.let { usr ->

                        val childUpdates = hashMapOf<String, Any>()

                        val database =
                            Firebase.database.reference // We get the reference to the database
                        val userPath = database.child("users")
                            .child(usr.uid) // We get the path to the user

                        usr.displayName?.let {
                            if (it.isNotBlank()) childUpdates["name"] = it
                        }

                        childUpdates["signedDevice"] = android_id

                        userPath.updateChildren(childUpdates).addOnSuccessListener {
                            Timber.e("Synced SIGNED DEVICE to Database successfully")
                        }.addOnFailureListener {
                            Timber.e("Offline, cannot sync SIGNED DEVICE")
                        }

                        firstPullFromDB(usr)
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    Timber.e( "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                    //startAuth()
                }

            }
    }*//*
    companion object {
        const val newPlayGames = false
    }

    private fun convertAccountToPlayGames() {
        Firebase.auth.currentUser?.let {
            try {
                val userPath = Firebase.database.reference.child("users").child(it.uid)
                userPath.removeValue().addOnSuccessListener {
                    Toast.makeText(
                        this,
                        getString(R.string.old_account_disabled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            try {
                it.delete()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        startAuth(forceGooglePlay = true)
    }

    *//*if (newPlayGames) {
           PlayGamesSdk.initialize(this)

           val gamesSignInClient = PlayGames.getGamesSignInClient(this)

           gamesSignInClient.signIn().addOnSuccessListener {
               gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
                   val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                           isAuthenticatedTask.result.isAuthenticated
                   if (isAuthenticated) {
                       // Continue with Play Games Services
                       gamesSignInClient
                           .requestServerSideAccess(
                               "125303088755-peil3n817pq3srcvhikmokqulu5j5l0p.apps.googleusercontent.com",
                                   false
                               )
                               .addOnCompleteListener { task: Task<String?> ->
                                   if (task.isSuccessful) {
                                       val serverAuthToken = task.result

                                       serverAuthToken?.let {
                                           firebaseAuthWithPlayGames(it)
                                       }
                                       // Send authentication code to the backend game server to be
                                       // exchanged for an access token and used to verify the
                                       // player via the Play Games Services REST APIs.
                                   } else {
                                       // Failed to retrieve authentication code.
                                       Toast.makeText(this, "Failed to retrieve auth code", Toast.LENGTH_SHORT).show()
                                   }
                               }

                       } else {
                           // Disable your integration with Play Games Services or show a
                           // login button to ask  players to sign-in. Clicking it should
                           // call GamesSignInClient.signIn().
                           startAuth()
                       }
                   }
               }
           } else {

           }*//*

    private fun startMainActivity(saveOnCreate: Boolean, saveCreationDate: Boolean) {

        if (saveCreationDate) {
            var creationTimestamp: Long = System.currentTimeMillis()

            mAuth.currentUser?.let { usr ->
                val database = Firebase.database.reference

                val creationDatePath = database.child("users").child(usr.uid).child("creation_date")

                if (usr.metadata != null && usr.metadata?.creationTimestamp != null) {
                    creationTimestamp = usr.metadata!!.creationTimestamp
                }
                creationDatePath.child("creation_timestamp").setValue(creationTimestamp)


                dataSaved.edit().apply {
                    putLong("creation_timestamp", creationTimestamp)
                    putBoolean("firstLaunch", false)
                }.apply()
            }


            val intent = Intent(this@AuthActivity, MainActivity::class.java)
            if (saveOnCreate) intent.putExtra("saveOnCreate", true)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this@AuthActivity, MainActivity::class.java)
            if (saveOnCreate) intent.putExtra("saveOnCreate", true)
            startActivity(intent)
            finish()
        }

        fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
            val spannableString = SpannableString(this.text)
            var startIndexOfLink = -1
            for (link in links) {
                val clickableSpan = object : ClickableSpan() {
                    override fun updateDrawState(textPaint: TextPaint) {
                        // use this to change the link color
                        textPaint.color = textPaint.linkColor
                        // toggle below value to enable/disable
                        // the underline shown below the clickable text
                        textPaint.isUnderlineText = true
                    }

                    override fun onClick(view: View) {
                        Selection.setSelection((view as TextView).text as Spannable, 0)
                        view.invalidate()
                        link.second.onClick(view)
                    }
                }
                startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)

                spannableString.setSpan(
                    clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            this.movementMethod =
                LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
            this.setText(spannableString, TextView.BufferType.SPANNABLE)
        }

        fun getS(i: Int) = resources.getString(i)
    }*/
}