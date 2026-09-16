# Skill: Generador de Módulos de Pantalla (Feature Generator)

## Propósito
Esta skill define la estructura estándar para construir cualquier nuevo módulo o pantalla dentro de LifterLab, asegurando que se cumpla la arquitectura MVVM, ViewBinding, Kotlin Coroutines y la integración con Firebase Firestore descrita en `agent.md`.

---

## Instrucciones para el Agente de IA

Cuando se te solicite implementar un Requerimiento Funcional (ej. "Implementa el RF08: Series de aproximación"), debes generar los siguientes 4 archivos manteniendo la siguiente convención de nombres y responsabilidad:

### 1. Modelo de Estado de la UI (`UiState`)
Ubicación: `ui/features/<feature_name>/<Feature>UiState.kt`
- Debe ser una `sealed interface` o `data class` inmutable que represente todos los estados de la pantalla.
- Campos obligatorios para carga y error: `isLoading: Boolean = false`, `errorMessage: String? = null`.

### 2. ViewModel (`ViewModel`)
Ubicación: `ui/features/<feature_name>/<Feature>ViewModel.kt`
- Hereda de `ViewModel()`.
- Expone el estado a través de un `StateFlow` privado mutable y uno público inmutable (`StateFlow<UiState>`).
- Maneja la lógica de negocio y llamadas al repositorio mediante `viewModelScope.launch`.

### 3. Repositorio / Servicio de Firestore (`Repository`)
Ubicación: `data/repository/<Feature>Repository.kt`
- Realiza consultas asíncronas a Cloud Firestore usando `suspend` y `await()`.
- Todas las rutas de Firestore DEBEN construirse a partir de la raíz `/users/{userId}/...` para respetar la regla de aislamiento del usuario (`request.auth.uid == userId`).
- Retorna siempre un tipo `Result<T>` (`Result.success` o `Result.failure`).

### 4. Layout XML y Fragment (`Fragment` + `XML`)
Ubicación: `ui/features/<feature_name>/<Feature>Fragment.kt` y `res/layout/fragment_<feature_name>.xml`
- El XML debe usar `ConstraintLayout` y componentes de Material Design 3.
- El Fragment debe implementar `ViewBinding` asignando la variable en `onCreateView` y nulleándola en `onDestroyView()`.
- Debe suscribirse al `StateFlow` del ViewModel dentro de un bloque `viewLifecycleOwner.lifecycleScope.launch`.

---

## Plantilla de Código Base a seguir por el Agente

```kotlin
// Ej. Plantilla de Fragment con ViewBinding y StateFlow
class FeatureFragment : Fragment() {

    private var _binding: FragmentFeatureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeatureViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Array<out Any>?) {
        super.onViewCreated(view, savedInstanceState)
        observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Actualizar vistas mediante binding
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}