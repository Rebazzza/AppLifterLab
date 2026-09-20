# Skill: Generador de Vistas Simples en Kotlin

## Propósito
Definir la estructura mínima y directa para crear pantallas en LifterLab escribiendo únicamente código en Kotlin nativo, sin usar XML, ViewBinding ni ViewModels.

---

## Instrucciones para el Agente de IA

Cuando se solicite implementar una pantalla o función (ej. "Implementa el RF08: Series de aproximación"), genera **un único archivo Kotlin** siguiendo esta estructura básica:

### Ubicación y Nombre
`ui/features/<Feature>Activity.kt` o `ui/features/<Feature>View.kt`

### Reglas de Diseño y Código
- **Sin XML ni ViewBinding:** Toda la interfaz se crea mediante código Kotlin usando vistas simples (`LinearLayout`, `TextView`, `Button`, `EditText`).
- **Sin ViewModel:** La lógica y el manejo de datos ocurren directamente en la clase/Activity.
- **Sin Repositorio separado:** Las llamadas a Firebase Firestore se hacen directamente en el archivo Kotlin usando funciones `suspend` o listeners básicos.
- **Estilo simple:** Usa parámetros básicos de Kotlin para layout (`layoutParams`), colores simples e inputs directos.

---

## Plantilla de Código Base a seguir por el Agente

```kotlin
class FeatureActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Contenedor principal vertical simple
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Título de la pantalla
        val title = TextView(this).apply {
            text = "Nombre de la Pantalla"
            textSize = 20f
        }

        // Campo de entrada básico
        val input = EditText(this).apply {
            hint = "Ingrese un valor"
        }

        // Botón de acción directo con consulta a Firestore
        val button = Button(this).apply {
            text = "Guardar"
            setOnClickListener {
                val data = hashMapOf("valor" to input.text.toString())
                
                // Guardar directo en Firestore bajo /users/{userId}/...
                db.collection("users").document(userId)
                    .collection("datos").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Guardado con éxito", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Agregar elementos al layout y mostrar
        layout.addView(title)
        layout.addView(input)
        layout.addView(button)

        setContentView(layout)
    }
}