package com.example.logementsadmin

// Import pour les fonctionnalites de base d'une page Android
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// Classe principale qui s'execute au demarrage de l'application
class MainActivity : AppCompatActivity() {

    // Fonction appelee automatiquement quand la page se cree
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cree un ordre de navigation vers la page de connexion
        val intent = Intent(this, LoginActivity::class.java)

        // Lance la page de connexion
        startActivity(intent)

        // Ferme MainActivity pour ne pas pouvoir revenir en arriere
        finish()
    }
}