package com.example.logementsadmin

// Import pour la navigation entre pages
import android.content.Intent
// Import pour les fonctionnalites de base d'une page Android
import androidx.appcompat.app.AppCompatActivity
// Import pour le bundle de donnees au demarrage
import android.os.Bundle
// Import pour les boutons
import android.widget.Button

// Classe qui gere la page du menu principal
class DashboardActivity : AppCompatActivity() {

    // Declaration des boutons de l'interface
    private lateinit var buttonUsers: Button
    private lateinit var buttonLogements: Button
    private lateinit var buttonLogout: Button

    // Fonction appelee automatiquement quand la page se cree
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definit l'interface a afficher
        setContentView(R.layout.activity_dashboard)

        // Recupere les boutons par leur identifiant
        buttonUsers = findViewById(R.id.buttonUsers)
        buttonLogements = findViewById(R.id.buttonLogements)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Quand on clique sur "Gerer les utilisateurs"
        buttonUsers.setOnClickListener {
            // Cree un ordre de navigation vers la page des utilisateurs
            val intent = Intent(this, UsersActivity::class.java)
            // Lance la page des utilisateurs
            startActivity(intent)
        }

        // Quand on clique sur "Gerer les logements"
        buttonLogements.setOnClickListener {
            // Cree un ordre de navigation vers la page des logements
            val intent = Intent(this, LogementsActivity::class.java)
            // Lance la page des logements
            startActivity(intent)
        }

        // Quand on clique sur "Se deconnecter"
        buttonLogout.setOnClickListener {
            // Cree un ordre de navigation vers la page de connexion
            val intent = Intent(this, LoginActivity::class.java)
            // Efface tout l'historique de navigation pour ne pas revenir en arriere
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Lance la page de connexion
            startActivity(intent)
        }
    }
}