package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CustomExercise(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val muscleEn: String,
    val muscleAr: String,
    val equipmentEn: String = "Bodyweight",
    val equipmentAr: String = "وزن الجسم",
    val difficultyEn: String = "Beginner",
    val difficultyAr: String = "مبتدئ",
    val instructionsEn: String = "Perform controlled movements securely with focus on tension and form.",
    val instructionsAr: String = "قم بتأدية التمرين بحركات محكومة وبتركيز كامل مع استهداف الانقباض العضلي."
)

data class ExerciseDef(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val muscleEn: String,
    val muscleAr: String,
    val equipmentEn: String = "Dumbbell",
    val equipmentAr: String = "دمبلز",
    val difficultyEn: String = "Intermediate",
    val difficultyAr: String = "متوسط",
    val instructionsEn: String = "",
    val instructionsAr: String = ""
)

@Composable
fun WorkoutTrackerScreen(
    isArabic: Boolean,
    viewModel: GymViewModel,
    activity: MainActivity
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Interactive category list
    val categories = if (isArabic) {
        listOf("الكل", "الصدر", "الظهر", "الأكتاف", "الأرجل", "الباي والسبس", "البطن والخصر", "كارديو")
    } else {
        listOf("All", "Chest", "Back", "Shoulders", "Legs", "Arms", "Abs", "Cardio")
    }
    var selectedCategory by remember { mutableStateOf(if (isArabic) "الكل" else "All") }
    var searchQuery by remember { mutableStateOf("") }

    val workoutProgressList by viewModel.workoutProgress.collectAsState()

    // Load custom exercises list state
    var customExercisesList by remember {
        mutableStateOf(loadCustomExercises(context))
    }

    // Comprehensive free, unlimited, professional 50+ exercises database
    val baseExercises = remember {
        listOf(
            // Chest / الصدر
            ExerciseDef("ch1", "Flat Bench Press", "بنش برس مستوي بالبار", "Chest", "الصدر", "Barbell", "بار", "Intermediate", "متوسط", "Lie flat on bench. Grip bar wider than shoulders, lower to chest controlled, and press up with explosive chest tension.", "استلقِ مستوياً على المقعد. امسك البار بقبضة أوسع من الكتف، أنزله نحو الصدر بتحكم، ثم ادفع للأعلى بقوة."),
            ExerciseDef("ch2", "Incline Dumbbell Press", "تجميع دمبل عالي مائل", "Chest", "الصدر", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Position bench at a 30-45 degree incline. Raise dumbbells over your chest, lower controlled to collarbone level, and squeeze chest at top.", "اضبط المقعد بزاوية مائلة 30-45 درجة. ارفع الدمبلز فوق مستوى الصدر، ثم اهبط بها ببطء واضغط العضلة بالأعلى."),
            ExerciseDef("ch3", "Decline Bench Press", "بنش برس مائل لأسفل", "Chest", "الصدر", "Barbell", "بار", "Advanced", "متقدم", "Lie back on decline bench. Lower bar carefully to lower chest, press upward maintaining perfect pectoral tension.", "استلقِ على دكة مائلة لأسفل. أنزل البار تدريجياً نحو أسفل الصدر مع دفع البار بقبضة متوازنة."),
            ExerciseDef("ch4", "Cable Crossover Flyes", "تقاطع كابل الصدر", "Chest", "الصدر", "Cable", "كابل", "Intermediate", "متوسط", "Stand central between cable stacks. Bring hands together in a wide hugging motion, squeezing inner pectorals intensely.", "قف في منتصف جهاز الكابل. اسحب القوابض للأمام بحركة شبه دائرية مفرودة مع عصر عضلة الصدر الداخلية."),
            ExerciseDef("ch5", "Classic Push-Ups", "تمرين الضغط الكلاسيكي", "Chest", "الصدر", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Keep body linear. Lower torso until chest is an inch from the ground, push back up while engaging core shoulders and chest.", "حافظ على استقامة الجسم. أنزل صدرك ليقترب من الأرض، ثم ادفع للأعلى بقوة الصدر والأكتاف."),
            ExerciseDef("ch6", "Pec Deck Fly Machine", "تجميع الفراشة على الجهاز", "Chest", "الصدر", "Machine", "جهاز", "Beginner", "مبتدئ", "Adjust seat height. Squeeze padded arms or handles together, maintaining a slight bend in your elbows and dynamic contraction.", "اضبط الكرسي. اسحب الذراعين الدائريتين معاً أمام وجهك للحصول على انقباض قوي وثابت لعضلة الصدر الوسطى."),
            ExerciseDef("ch7", "Parallel Bar Dips", "المتوازي لأسفل الصدر", "Chest", "الصدر", "Bodyweight", "وزن الجسم", "Advanced", "متقدم", "Grip bars, lean chest slightly forward. Lower body bending elbows, then press upward keeping shoulders safe.", "امسك المقودين، انحنِ قليلاً للأمام بجذعك. اهبط بثني الكوعين ثم ادفع بقوة وثبات عضلات الصدر السفلي."),

            // Back / الظهر
            ExerciseDef("bk1", "Barbell Deadlifts", "الرفعة المميتة بالبار", "Back", "الظهر", "Barbell", "بار", "Advanced", "متقدم", "Position bar at mid-foot. Hinge forward keeping spine perfectly straight, lift bar driving hips forward to upright lock.", "ضع البار في منتصف مشط القدم. انحنِ للأمام بظهر مستقيم تماماً، واقفل الركبة والحوض بالأعلى باحتراف."),
            ExerciseDef("bk2", "Strict Pull-Ups", "تمرين العقلة الصارم", "Back", "الظهر", "Bodyweight", "وزن الجسم", "Advanced", "متقدم", "Hang with wide grip. Pull elbows down and back to lift your upper chest over the bar, lower smoothly to active hang.", "تعلق بقبضة يد واسعة. اسحب جسمك للأعلى من خلال الكوعين حتى تعلو ذقنك فوق البار، ثم اهبط ببطء."),
            ExerciseDef("bk3", "Lat Pulldown Machine", "سحب كابل لأسفل واسع", "Back", "الظهر", "Machine", "جهاز", "Beginner", "مبتدئ", "Sit firmly. Secure knees, pull bar under chin towards upper clavicles while squeezing lat muscles in back.", "اجلس وثبت الفخذين. اسحب البار لأسفل نحو صدرك العلوي، مع شد عضلات الظهر الجانبية المجنص."),
            ExerciseDef("bk4", "Bent Over Barbell Row", "سحب بار منحني للظهر", "Back", "الظهر", "Barbell", "بار", "Intermediate", "متوسط", "Hinge hips back at 45 degrees. Pull barbell to lower rib cages keeping back stable and flat.", "انحنِ بالحوض للخلف 45 درجة بظهر مسطح ومفرود. اسحب البار لقرب عضلات البطن العليا."),
            ExerciseDef("bk5", "One-Arm Dumbbell Row", "سحب دمبل فردي منشار", "Back", "الظهر", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Rest knee and hand on flat bench. Pull dumbbell to hip targetting lat muscle with full stretch.", "ضع ركبة ويداً واحدة على الدكة للتثبيت. اسحب الدمبل بتركيز كوعك للأعلى نحو الخصر ملامساً الظهر."),
            ExerciseDef("bk6", "Seated Cable Row", "سحب كابل جالس للظهر", "Back", "الظهر", "Cable", "كابل", "Beginner", "مبتدئ", "Sit stable with flat back. Pull handle towards abdomen, squeezing shoulder blades together tightly.", "اجلس بظهر مستقيم تماماً. اسحب المقبض نحو السرة واضغط لوحي كتفك بقوة."),
            ExerciseDef("bk7", "T-Bar rows", "سحب بار T للظهر", "Back", "الظهر", "Barbell", "بار", "Intermediate", "متوسط", "Hold handle bar with weight. Pull close to body driving elbows high to build back thickness.", "قف بظهر مستقيم ومنحنٍ فوق بار T. اسحب المقاود بقوتك للأعلى مع عصر العضلة الموجهة بمنتصف الظهر."),
            ExerciseDef("bk8", "Back Hyperextensions", "فرد الظهر على المنصة", "Back", "الظهر", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Lie on extension platform. Bend torso down, raise back up contracting lower back area and glutes.", "استلقِ على المنصة. انحنِ لأسفل ثم ارتفع بجذعك ببطء للأعلى مستهدفاً الفقرات القطنية والظهر السفلي."),

            // Shoulders / الأكتاف
            ExerciseDef("sh1", "Overhead Military Press", "الضغط العسكري بالبار واقفاً", "Shoulders", "الأكتاف", "Barbell", "بار", "Advanced", "متقدم", "Press barbell from upper chest level straight up over head without dipping knees. Brace abdominal walls.", "ادفع البار من مستوى أعلى الصدر في خط مستقيم للأعلى فوق الرأس مع المحافظة على ثبات الجسم والبطن."),
            ExerciseDef("sh2", "Dumbbell Shoulder Press", "ضغط أكتاف دمبل جالساً", "Shoulders", "الأكتاف", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Sit on upright bench. Push dumbbells upward from shoulder height till arms are fully vertical and linear.", "اجلس بثبات على الكرسي. ادفع الدمبلز للأعلى حتى يمتد ذراعيك بالكامل مع الحفاظ على التوازن."),
            ExerciseDef("sh3", "Dumbbell Lateral Raise", "رفرفة دمبل جانبي للأكتاف", "Shoulders", "الأكتاف", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Stand straight. Lift dumbbells sideways with slight elbow bend up to shoulder height to isolate lateral deltoids.", "قف مستقيماً وثابتاً. ارفع الدمبلز جانباً ببطء حتى مستوى كتفيك لعزل وتجريح الأكتاف الجانبية."),
            ExerciseDef("sh4", "Front Dumbbell Raise", "رفرفة دمبل أمامي للأكتاف", "Shoulders", "الأكتاف", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Raise dumbbells forward one by one to shoulder height keeping arms straight. Control the descend.", "ارفع دمبلاً واحداً أو الاثنين للأمام بالتبادل حتى مستوى النظر وبسرعة بطيئة في الهبوط للضغط."),
            ExerciseDef("sh5", "Bent-Over Rear Delt Fly", "رفرفة خلفي دمبل منحني", "Shoulders", "الأكتاف", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Hinge waist. Raise dumbbells out to sides, contracting shoulder blades and isolating rear deltoid heads.", "انحنِ للأمام من الحوض للأسفل. ارفع الدمبلز جانباً للأعلى لعزل الأكتاف الخلفية الصغيرة بنجاح."),
            ExerciseDef("sh6", "EZ-Bar Upright Row", "سحب بار زجزاج عمودي", "Shoulders", "الأكتاف", "Barbell", "بار", "Intermediate", "متوسط", "Pull barbell close to body up to upper chest, lifting elbows maximum outwards. Feel traps and deltoids.", "اسحب البار للأعلى محاذاً للبطن والصدر، وارفع كوعك للخارج لأقصى حد لاستهداف الترايبس والترابيس."),
            ExerciseDef("sh7", "Dumbbell Shrugs", "تمرين الترباس بالدمبلز", "Shoulders", "الأكتاف", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Hold heavy weights. Shrug shoulders straight up as high as possible, hold, then lower slowly.", "امسك أوزاناً كافية وثقيلة. ارفع كتفيك للأعلى باتجاه الأذن واهبط بذكاء وسلامة لعضلات الترابيس."),
            ExerciseDef("sh8", "Cable Face Pulls", "سحب كابل خلفي للوجه", "Shoulders", "الأكتاف", "Cable", "كابل", "Beginner", "مبتدئ", "Attach rope. Pull to forehead level splitting hands and clamping rear shoulders and upper back.", "اسحب الحبل من الكابل بشكل مستوٍ نحو وجهك وجبهتك مع تفريق الكفين وعصر الكتف الخلفي."),

            // Legs / الأرجل
            ExerciseDef("lg1", "Barbell Back Squats", "سكوات بار خلفي عميق", "Legs", "الأرجل", "Barbell", "بار", "Advanced", "متقدم", "Load barbell. Hinge hips back, squat deep keeping knees tracking over toes, push up using heels.", "ضع بار الأوزان على عضلات الرقبة الخلفية. انزل بالحوض للخلف والأسفل كوضعية جلوس، ثم ادفع للأعلى من كعب القدم."),
            ExerciseDef("lg2", "Bulgarian Split Squat", "سكوات بلغاري فردي", "Legs", "الأرجل", "Dumbbell", "دمبلز", "Advanced", "متقدم", "Place back foot on bench. Lower hips on single front leg until thigh is parallel to ground.", "ضع مشط قدمك الخلفية على دكة. انزل بالقدم الأمامية المستوية لأسفل حتى يصنع الفخذ زاوية 90 درجة مع دفع كعبك."),
            ExerciseDef("lg3", "Leg Press Machine", "جهاز دفع أرجل مائل", "Legs", "الأرجل", "Machine", "جهاز", "Beginner", "مبتدئ", "Press platform away dynamically. Do not lock knees totally at peak, pull back controlled.", "اضغط اللوح الحديدي بعضلات الفخذ والمؤخرة بقوة. تجنب غلق الركبة تماماً لتفادي الخطر ثم اهبط ببطء."),
            ExerciseDef("lg4", "Leg Extensions", "جهاز رفرفة أرجل أمامي", "Legs", "الأرجل", "Machine", "جهاز", "Beginner", "مبتدئ", "Sit with pad on lower ankles. Lift legs straight until parallel to ground, flexing quadriceps.", "اجلس على كرسي الجهاز. افرد القدمين للأعلى حتى يستقيم الساقان واضغط عضلات الفخذ الأمامية (الرباعية) بقوة."),
            ExerciseDef("lg5", "Romanian Deadlift (RDL)", "رفعة مميتة رومانية للرجل", "Legs", "الأرجل", "Barbell", "بار", "Intermediate", "متوسط", "Grip barbell, bend knees minimally. Hinge hips backwards to feel deep stretch across hamstrings.", "امسك البار وابقِ الركب مفرودة ومثنية خفيفاً. ارجع بالحوض للخلف لتشعر بإطالة قصوى بالخلفيات وعضلات المؤخرة."),
            ExerciseDef("lg6", "Lying Leg Curls", "جهاز ثني أرجل خلفي مستلقي", "Legs", "الأرجل", "Machine", "جهاز", "Beginner", "مبتدئ", "Lie flat on belly. Flex ankles upward against roller, driving heels to glutes to squeeze hamstrings.", "استلقِ على وجهك بجهاز الخلفية. اثنِ الساقين للأعلى بقوة محاولاً ملامسة السمانة للفخذ ثم انزل ببطء."),
            ExerciseDef("lg7", "Standing Calf Raises", "تمرين سمانة واقف أوزان", "Legs", "الأرجل", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Stand on block edge. Lower heels beyond block level and raise straight up onto toes maximum.", "قف بمقدمة القدم على حافة سطح خشبي. انزل بالكعبين لامتصاص الشد ثم ارتفع لأعلى نقطة مفرجة للسمانة."),
            ExerciseDef("lg8", "Walking Dumbbell Lunges", "طعن مشي بالدمبلز للأرجل", "Legs", "الأرجل", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Step forward with dumbbells. Descend body until rear knee nearly clips flat soil, repeat on other side.", "تقدم خطوة للأمام حاملاً الدمبلز وانزل بجسمك مستقيماً حتى تطاول الركبة الخلفية الأرض واجلب توازناً."),

            // Arms / الذراعين الباي والسبس
            ExerciseDef("ar1", "Barbell Bicep Curls", "تبادل بايسبس بالبار واقفاً", "Arms", "الباي والسبس", "Barbell", "بار", "Beginner", "مبتدئ", "Grip bar shoulder wide. Curl upward squeezing biceps with tucked tight elbows.", "امسك البار بعرض الكتف. اثنِ ذراعك للأعلى وضاماً الكوعين على جانبي الخصر للحفاظ على العزل."),
            ExerciseDef("ar2", "Dumbbell Incline Curls", "تبادل بايسبس دمبل مائل خلفي", "Arms", "الباي والسبس", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Recline on 45 incline. Squeeze weights up rotating wrists outward to maximize bicep peak contraction.", "استلقِ على مقعد مائل للخلف. اثنِ الأوزان للأعلى مع تدوير يدك للخارج للحصول على أقصى إطالة للباي."),
            ExerciseDef("ar3", "Dumbbell Hammer Curls", "تبادل هامر شاكوش بالدمبلز", "Arms", "الباي والسبس", "Dumbbell", "دمبلز", "Beginner", "مبتدئ", "Curl dumbbells facing palm inwards. Build brachial forearm and bicep thickness.", "اثنِ الدمبلز حابساً حركة المعصم ليكون الكف للداخل كقبضة شاكوش، مستهدفاً الساعد والباي معاً."),
            ExerciseDef("ar4", "Preacher Bicep Curls", "بايسبس على منصة الواعظ", "Arms", "الباي والسبس", "Barbell", "بار", "Intermediate", "متوسط", "Rest arm pits on preacher pad. Elevate EZ bar carefully keeping torso completely stationary.", "ضع الذراعين على وسادة المقعد المخصص لعزل الحركة. اثنِ الوزن ببطء للأعلى ثم اهبط للآخر بتحكم."),
            ExerciseDef("ar5", "Cable Tricep Pushdown", "ترايسبس كابل لأسفل", "Arms", "الباي والسبس", "Cable", "كابل", "Beginner", "مبتدئ", "Hold cable bar or rope. Flex elbows downward straightening arms fully, squeezing the triceps head.", "امسك ببار الكابل أو حبل المقبض. ادفع لأسفل بالاعتماد على الترايسيبس وافرد الكوعين بتركيز شديد."),
            ExerciseDef("ar6", "Overhead Tricep Extension", "ترايسبس دمبل خلف الرأس", "Arms", "الباي والسبس", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Hold dumbbell over head using both hand grips. Bend at elbows to lower down behind neck then press.", "ارفع دمبلاً زوجياً باليدين فوق الرأس. انزل به خلف رأسك من ثني الكوعين ثم افرده ثانية للأعلى بقوة."),
            ExerciseDef("ar7", "Barbell Skull Crushers", "تفريد ترايسبس بار زجزاج مستلقي", "Arms", "الباي والسبس", "Barbell", "بار", "Advanced", "متقدم", "Lie flat, extend barbell forward. Fold elbows back bringing bar right over forehead bone, then press.", "استلقِ مستوياً وامسك بالبار المتعرج. اثنِ المرفقين فقط لتنزل بالبار قرب جبهتك ثم افردهما بقوة."),
            ExerciseDef("ar8", "Tricep Parallel Dips", "تمرين متوازي خلفي للترايسبس", "Arms", "الباي والسبس", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Grip edge of table or parallel bench. Keep body vertical, drop by bending handles, push to sky.", "ثبت يديك خلفاً على حافة دكة. انزل بالفخذين لتقريبهم من الأرض بثني الكوعين ثم ادفع بجسمك لفوق."),

            // Abs / البطن والخصر
            ExerciseDef("ab1", "Abdominal Crunches", "طحن البطن الكلاسيكي", "Abs", "البطن والخصر", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Lie knees bent. Lift shoulders slightly upward isolating core muscles, do not pull neck.", "استلقِ بركب مثنية وقدمين على الأرض. ارفع كتفيك للأعلى مستخدماً عضلات البطن مع تجنب سحب الرأس باليد."),
            ExerciseDef("ab2", "Hanging Leg Raises", "رفع الأرجل على العقلة للبطن", "Abs", "البطن والخصر", "Bodyweight", "وزن الجسم", "Advanced", "متقدم", "Hang from bar hands wide. Lift long legs dynamically parallel to ground to strain lower abdominals.", "تعلق بالبار بقبضة قوية. ارفع ساقيك الممدودتين للأمام لتكون موازية لارتفاع الحوض، ثم أنزل تدريجياً."),
            ExerciseDef("ab3", "Weighted Russian Twists", "التواء روسي جانبي للخصر", "Abs", "البطن والخصر", "Dumbbell", "دمبلز", "Intermediate", "متوسط", "Sit tailbone balanced. Pivot weight left and right smoothly engaging diagonal obliques.", "اجلس على مؤخرتك طائراً بالقدمين للأعلى قليلاً. امسك بالوزن وادره يميناً ويساراً مع الالتفات التام للخصر."),
            ExerciseDef("ab4", "Static Core Plank", "تمرين البلانك للثبات الكلي", "Abs", "البطن والخصر", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Rest on forearms and toes. Flatten back dynamically and contract all stomach walls tightly.", "استقر على المرفقين ومشط القدم بظهر مفرود وموازٍ تماماً للأرض مع شد عضلات البطن لمدة من الزمن."),
            ExerciseDef("ab5", "Bicycle Crunches", "دراجة البطن التبادلية عميقة", "Abs", "البطن والخصر", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Alternate touching elbows to opposite knees in cycling motion.", "المس بمرفقك الأيمن الركبة اليسرى وبالعكس بشكل تبادلي وسريع لاستهداف عضلات الخصر والبطن."),

            // Cardio / الكارديو والجسم كامل
            ExerciseDef("cd1", "Dynamic Burpees", "تمرين البيربي لحرق الدهون", "Cardio", "كارديو", "Bodyweight", "وزن الجسم", "Advanced", "متقدم", "Squat lower, push feet out into push up stance and dynamic spring back up with vertical high jump.", "انحنِ لأسفل، ادفع قدميك للخلف كوضعية الضغط، قم بضغطة واحدة، ثم تفجر للأعلى بقفزة عمودية قصوى."),
            ExerciseDef("cd2", "Kettlebell Swings", "أرجحة كيتل بيل لأسفل الظهر والبور", "Cardio", "كارديو", "Kettlebell", "كيتل بيل", "Intermediate", "متوسط", "Thrust kettlebell forward to eye length powered by hamstrings core and posterior chains.", "امسك مقبض الكيتل بيل وادفعه للأمام بقوة عضلات الحوض والظهر والسمانة بحركة أرجحة ممتدة."),
            ExerciseDef("cd3", "Mountain Climbers", "تمرين تسلق الجبل السريع", "Cardio", "كارديو", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Run knees sequentially to chest in rapid motion from high push up board position.", "قف بشكل موازٍ للأرض وارتكز على يديك. تبادل جلب ركبتيك لصدرك بسرعة بدفع تبادلي للحرق السريع."),
            ExerciseDef("cd4", "Speed Jumping Rope", "نط الحبل السريع للياقة القلبية", "Cardio", "كارديو", "Bodyweight", "وزن الجسم", "Beginner", "مبتدئ", "Skip in vertical rhythmic jumps over swinging line rope for maximal aerobic lung capacity.", "اقفز بثبات وتناسق حركي لتسمح للحبل بالمرور تحت قدميك، مع تعزيز صحة القلب.")
        )
    }

    // Assemble absolute active set (Standard + Custom loaded securely)
    val allExercises = remember(customExercisesList) {
        val mappedCustom = customExercisesList.map { 
            ExerciseDef(
                id = it.id, 
                nameEn = it.nameEn, 
                nameAr = it.nameAr, 
                muscleEn = it.muscleEn, 
                muscleAr = it.muscleAr,
                equipmentEn = it.equipmentEn,
                equipmentAr = it.equipmentAr,
                difficultyEn = it.difficultyEn,
                difficultyAr = it.difficultyAr,
                instructionsEn = it.instructionsEn,
                instructionsAr = it.instructionsAr
            ) 
        }
        baseExercises + mappedCustom
    }

    // Category sorting and search matcher
    val filteredExercises = remember(allExercises, selectedCategory, searchQuery, isArabic) {
        allExercises.filter { ex ->
            // Category Match
            val matchCategory = when (selectedCategory) {
                "الكل", "All" -> true
                "الصدر", "Chest" -> ex.muscleEn.equals("Chest", ignoreCase = true) || ex.muscleAr.contains("الصدر")
                "الظهر", "Back" -> ex.muscleEn.equals("Back", ignoreCase = true) || ex.muscleAr.contains("الظهر")
                "الأكتاف", "Shoulders" -> ex.muscleEn.equals("Shoulders", ignoreCase = true) || ex.muscleAr.contains("الأكتاف")
                "الأرجل", "Legs" -> ex.muscleEn.contains("Legs", ignoreCase = true) || ex.muscleAr.contains("الأرجل")
                "الباي والسبس", "Arms" -> ex.muscleEn.equals("Arms", ignoreCase = true) || ex.muscleEn.contains("Biceps", ignoreCase = true) || ex.muscleEn.contains("Triceps", ignoreCase = true) || ex.muscleAr.contains("الباي والسبس") || ex.muscleAr.contains("الذراع") || ex.muscleEn.equals("Arms", ignoreCase = true)
                "البطن والخصر", "Abs" -> ex.muscleEn.equals("Abs", ignoreCase = true) || ex.muscleEn.equals("Core", ignoreCase = true) || ex.muscleAr.contains("البطن") || ex.muscleAr.contains("الخصر")
                "كارديو", "Cardio" -> ex.muscleEn.equals("Cardio", ignoreCase = true) || ex.muscleAr.contains("كارديو") || ex.muscleAr.contains("كامل") || ex.muscleAr.contains("الكارديو")
                else -> true
            }

            // Search Query Match (Supports Bilingual Names, Muscles, Equipment)
            val matchSearch = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                ex.nameEn.contains(searchQuery, ignoreCase = true) ||
                ex.nameAr.contains(searchQuery, ignoreCase = true) ||
                ex.muscleEn.contains(searchQuery, ignoreCase = true) ||
                ex.muscleAr.contains(searchQuery, ignoreCase = true) ||
                ex.equipmentEn.contains(searchQuery, ignoreCase = true) ||
                ex.equipmentAr.contains(searchQuery, ignoreCase = true) ||
                ex.difficultyEn.contains(searchQuery, ignoreCase = true) ||
                ex.difficultyAr.contains(searchQuery, ignoreCase = true)
            }

            matchCategory && matchSearch
        }
    }

    // High end rest timers
    var showTimer by remember { mutableStateOf(false) }
    var timerSecondsLeft by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, timerSecondsLeft) {
        if (isTimerRunning && timerSecondsLeft > 0) {
            delay(1000L)
            timerSecondsLeft -= 1
            if (timerSecondsLeft == 0) {
                isTimerRunning = false
                showTimer = false
                activity.playSuccessBeep()
                activity.speak(if (isArabic) "انتهى وقت الراحة، استعد للمجموعة القادمة" else "Rest is over, prepare for the next set", isArabic)
            }
        }
    }

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var isFetchingCoachAdvice by remember { mutableStateOf(false) }
    var coachAdviceText by remember { mutableStateOf("") }

    // Dialog State to Delete Custom Exercises
    var exerciseToDelete by remember { mutableStateOf<ExerciseDef?>(null) }

    // Safety Dialog state to confirm resetting data
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workout_screen_lazycolumn"),
        contentPadding = PaddingValues(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 120.dp)
    ) {
        item {
            // --- 2. CONTROL DECK GRID (Advice, Add Custom, Reset) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Intelligent interactive AI advice
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by if (isFetchingCoachAdvice) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.98f,
                        targetValue = 1.02f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "advice_pulse"
                    )
                } else {
                    remember { mutableStateOf(1f) }
                }

                Button(
                    onClick = {
                        if (!isFetchingCoachAdvice) {
                            isFetchingCoachAdvice = true
                            coachAdviceText = ""
                            coroutineScope.launch {
                                try {
                                    coachAdviceText = viewModel.aiAssistant.getCoachAdvice(selectedCategory, isArabic)
                                    activity.triggerVibration(80)
                                } catch (e: Exception) {
                                    coachAdviceText = if (isArabic) {
                                        "العب بوزن مناسب وركز على الانقباض الكامل للعضلة لتفجير الأنسجة وحصد الضخ المطلوب! 🔥"
                                    } else {
                                        "Perform controlled eccentric stretches with progressive load to trigger absolute myofibrillar hypertrophy! 🔥"
                                    }
                                } finally {
                                    isFetchingCoachAdvice = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .scale(pulseScale)
                        .testTag("coach_advice_btn"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Trophy, 
                        contentDescription = null, 
                        tint = CyberCharcoal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "نصيحة الكابتن" else "Coach AI Advice",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
    
                // Custom patterns creator
                Button(
                    onClick = { 
                        showAddCustomDialog = true 
                        activity.triggerVibration(45)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RockGray, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_custom_exercise_btn")
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Add, 
                        contentDescription = null, 
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "تمرين مخصص" else "+ Custom",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
    
                // High contrast danger reset (triggers safe confirm modal)
                IconButton(
                    onClick = { 
                        activity.triggerVibration(100)
                        showResetConfirmDialog = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reset_workouts_btn")
                        .background(Color(0xFF2C1618), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Remove,
                        contentDescription = "Reset All Progress",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
    
            // --- 3. DYNAMIC AI RESPONSIVE TIPS PLATE ---
            AnimatedVisibility(
                visible = coachAdviceText.isNotEmpty() || isFetchingCoachAdvice,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.82f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.82f)) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .iosSquircleCard(
                            cornerSizeRatio = 0.24f,
                            baseOpacity = 0.18f,
                            saturationScale = 1.80f
                        )
                        .padding(16.dp)
                ) {
                    if (isFetchingCoachAdvice) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = NeonYellow, 
                                modifier = Modifier.size(18.dp), 
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "يتم جلب استراتيجية التدريب..." else "AI Coach generating elite tips...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Flame,
                                    contentDescription = null,
                                    tint = NeonYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "توجيه الكابتن الذكي" else "AI GYM COMMANDER PANEL", 
                                    color = NeonYellow, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = PhosphorIcons.Remove, 
                                    contentDescription = "Dismiss", 
                                    tint = DimGray, 
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { coachAdviceText = "" }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = coachAdviceText,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
    
            Spacer(modifier = Modifier.height(10.dp))

            // --- 4. ULTRADYNAMIC HIGH-PERFORMANCE SEARCH & FILTER BAR ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_search_input"),
                        placeholder = { 
                            Text(
                                text = if (isArabic) "ابحث باسم التمرين، العضلة أو الأداة..." else "Search exercise, muscle, or equipment...", 
                                color = DimGray,
                                fontSize = 13.sp
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = "Search", 
                                tint = NeonYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear, 
                                        contentDescription = "Clear Search", 
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // High fidelity Real-time Database Results Counter tag
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isArabic) {
                            "تم العثور على ${filteredExercises.size} حركة عضلية مخصصة"
                        } else {
                            "${filteredExercises.size} elite fiber patterns cataloged"
                        },
                        color = NeonYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
    
            // --- 5. TACTICAL MUSCLE CAPSULES BAR ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val capsuleScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "capsule_animation"
                    )

                    val selectionBg = if (isSelected) NeonYellow else Color(0x2EFFFFFF)
                    val selectionBorder = if (isSelected) NeonYellow else Color.White.copy(alpha = 0.08f)
                    val selectionTextCol = if (isSelected) CyberCharcoal else Color.White
                    
                    // Assign a crisp vector icon to each category dynamically
                    val categoryIcon = when (category) {
                        "الكل", "All" -> PhosphorIcons.Weight
                        "الصدر", "Chest" -> PhosphorIcons.Dumbbell
                        "الظهر", "Back" -> PhosphorIcons.Dumbbell
                        "الأكتاف", "Shoulders" -> PhosphorIcons.Dumbbell
                        "الأرجل", "Legs" -> PhosphorIcons.Dumbbell
                        "الباي والسبس", "Arms" -> PhosphorIcons.Dumbbell
                        "البطن والخصر", "Abs" -> PhosphorIcons.Leaf
                        "كارديو", "Cardio" -> PhosphorIcons.Flame
                        else -> PhosphorIcons.Dumbbell
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .scale(capsuleScale)
                            .clip(SquircleShape(0.48f))
                            .background(selectionBg)
                            .border(1.dp, selectionBorder, SquircleShape(0.48f))
                            .clickable { 
                                selectedCategory = category 
                                activity.triggerVibration(40)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = selectionTextCol,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = category,
                                color = selectionTextCol,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
    
            Spacer(modifier = Modifier.height(10.dp))
    
            // --- 6. COMPACT EXTREME TIMING MODULE HUD ---
            AnimatedVisibility(
                visible = showTimer,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.8f)) + fadeOut()
            ) {
                Box(modifier = Modifier.padding(vertical = 6.dp)) {
                    CircularRestTimerModule(
                        secondsLeft = timerSecondsLeft,
                        isRunning = isTimerRunning,
                        isArabic = isArabic,
                        onPresetSelect = { preset -> 
                            timerSecondsLeft = preset
                            isTimerRunning = true
                            activity.triggerVibration(60)
                        },
                        onAdjustTimer = { delta ->
                            timerSecondsLeft = maxOf(0, timerSecondsLeft + delta)
                            activity.triggerVibration(40)
                        },
                        onPlayPause = { 
                            isTimerRunning = !isTimerRunning 
                            activity.triggerVibration(50)
                        },
                        onSkip = { 
                            isTimerRunning = false
                            showTimer = false
                            activity.triggerVibration(80)
                        }
                    )
                }
            }
        }
        
        // --- 7. SCROLLABLE TILE PATTERNS SHEET ---
        if (filteredExercises.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = PhosphorIcons.Dumbbell, 
                            contentDescription = null, 
                            tint = DimGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isArabic) "لا توجد تمارين تطابق خيارات البحث للفئة المحددة" else "No matching fiber exercises cataloged",
                            color = DimGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredExercises, key = { it.id }) { exercise ->
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    val progress = workoutProgressList.find { it.exerciseId == exercise.id } ?: WorkoutProgress(exercise.id)
    
                    ModernExerciseCard(
                        exercise = exercise,
                        progress = progress,
                        isArabic = isArabic,
                        context = context,
                        activity = activity,
                        onSetChecked = { setIndex, isChecked ->
                            val newProgress = when (setIndex) {
                                1 -> progress.copy(set1Done = isChecked)
                                2 -> progress.copy(set2Done = isChecked)
                                3 -> progress.copy(set3Done = isChecked)
                                else -> progress
                            }
                            viewModel.updateWorkout(newProgress)
                            activity.triggerVibration(60)
                            activity.playBeep()
                            
                            if (isChecked) {
                                showTimer = true
                                isTimerRunning = true
                                timerSecondsLeft = 60
                                val announcements = if (isArabic) {
                                    listOf("مجموعة رائعة! ارتاح ستين ثانية لتستعيد قوتك العضلية", "أداء بطل، تفجير عضلات ممتاز. فلتر تمرينك واسترح دقيقة")
                                } else {
                                    listOf("Phenomenal lift. Rest for 60 seconds of full anabolic recovery.", "Amazing set crushed. Rest mode active now.")
                                }
                                activity.speak(announcements.random(), isArabic)
                            }
                        },
                        onDeleteRequested = {
                            if (exercise.id.startsWith("custom_")) {
                                exerciseToDelete = exercise
                            }
                        }
                    )
                }
            }
        }
    }

    // --- INTERACTIVE CUSTOM ACTION SHEET DIALOG ---
    if (showAddCustomDialog) {
        var exNameEn by remember { mutableStateOf("") }
        var exNameAr by remember { mutableStateOf("") }
        var exMuscleEn by remember { mutableStateOf("Chest") }
        var exMuscleAr by remember { mutableStateOf("الصدر") }
        var exEquipmentEn by remember { mutableStateOf("Dumbbell") }
        var exEquipmentAr by remember { mutableStateOf("دمبلز") }
        var exDifficultyEn by remember { mutableStateOf("Intermediate") }
        var exDifficultyAr by remember { mutableStateOf("متوسط") }
        var exInstructionsEn by remember { mutableStateOf("") }
        var exInstructionsAr by remember { mutableStateOf("") }
        
        val muscleMapping = listOf(
            "Chest" to "الصدر",
            "Back" to "الظهر",
            "Shoulders" to "الأكتاف",
            "Legs" to "الأرجل",
            "Arms" to "الباي والسبس",
            "Abs" to "البطن والخصر",
            "Cardio" to "كارديو"
        )

        val equipmentMapping = listOf(
            "Barbell" to "بار",
            "Dumbbell" to "دمبلز",
            "Kettlebell" to "كيتل بيل",
            "Machine" to "جهاز",
            "Cable" to "كابل",
            "Bodyweight" to "وزن الجسم"
        )

        val difficultyMapping = listOf(
            "Beginner" to "مبتدئ",
            "Intermediate" to "متوسط",
            "Advanced" to "متقدم"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إدخال حركة مخصصة" else "INJECT PREFERATE PATTERN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = exNameAr,
                            onValueChange = { exNameAr = it },
                            label = { Text(if (isArabic) "الاسم باللغة العربية *" else "Arabic Title *") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonYellow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = NeonYellow,
                                unfocusedLabelColor = DimGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = exNameEn,
                            onValueChange = { exNameEn = it },
                            label = { Text(if (isArabic) "الاسم باللغة الانجليزية *" else "English Title *") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonYellow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = NeonYellow,
                                unfocusedLabelColor = DimGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = if (isArabic) "المجموعة العضلية التشريحية:" else "Anatomical Target Muscle:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(muscleMapping) { pair ->
                                val en = pair.first
                                val ar = pair.second
                                val activeSelection = exMuscleEn == en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activeSelection) NeonYellow else RockGray)
                                        .clickable {
                                            exMuscleEn = en
                                            exMuscleAr = ar
                                            activity.triggerVibration(40)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) ar else en,
                                        color = if (activeSelection) CyberCharcoal else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (isArabic) "الأداة التدريبية المستخدمة:" else "Training Equipment Type:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(equipmentMapping) { pair ->
                                val en = pair.first
                                val ar = pair.second
                                val activeSelection = exEquipmentEn == en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activeSelection) NeonYellow else RockGray)
                                        .clickable {
                                            exEquipmentEn = en
                                            exEquipmentAr = ar
                                            activity.triggerVibration(40)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) ar else en,
                                        color = if (activeSelection) CyberCharcoal else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (isArabic) "مستوى صعوبة التمرين:" else "Exercise Difficulty Level:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(difficultyMapping) { pair ->
                                val en = pair.first
                                val ar = pair.second
                                val activeSelection = exDifficultyEn == en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activeSelection) NeonYellow else RockGray)
                                        .clickable {
                                            exDifficultyEn = en
                                            exDifficultyAr = ar
                                            activity.triggerVibration(40)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) ar else en,
                                        color = if (activeSelection) CyberCharcoal else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = exInstructionsAr,
                            onValueChange = { exInstructionsAr = it },
                            label = { Text(if (isArabic) "طريقة أداء الحركة (عربي - اختياري)" else "Instructions (Arabic - Optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonYellow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = NeonYellow,
                                unfocusedLabelColor = DimGray
                            ),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = exInstructionsEn,
                            onValueChange = { exInstructionsEn = it },
                            label = { Text(if (isArabic) "طريقة أداء الحركة (إنجليزي - اختياري)" else "Instructions (English - Optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonYellow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = NeonYellow,
                                unfocusedLabelColor = DimGray
                            ),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exNameEn.isNotEmpty() && exNameAr.isNotEmpty()) {
                            val fctArIns = if (exInstructionsAr.trim().isEmpty()) {
                                if (isArabic) "أدِّ الحركة ببطء وتركيز تام على مدى الحركة الممتد." else "Perform movements carefully with controlled muscle contraction."
                            } else {
                                exInstructionsAr
                            }
                            val fctEnIns = if (exInstructionsEn.trim().isEmpty()) {
                                "Execute slowly under structural dynamic control with full athletic form."
                            } else {
                                exInstructionsEn
                            }
                            
                            val newEx = CustomExercise(
                                id = "custom_" + System.currentTimeMillis().toString(),
                                nameEn = exNameEn,
                                nameAr = exNameAr,
                                muscleEn = exMuscleEn,
                                muscleAr = exMuscleAr,
                                equipmentEn = exEquipmentEn,
                                equipmentAr = exEquipmentAr,
                                difficultyEn = exDifficultyEn,
                                difficultyAr = exDifficultyAr,
                                instructionsEn = fctEnIns,
                                instructionsAr = fctArIns
                            )
                            val modifiedList = customExercisesList + newEx
                            saveCustomExercises(context, modifiedList)
                            customExercisesList = modifiedList
                            showAddCustomDialog = false
                            activity.triggerVibration(100)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal)
                ) {
                    Text(if (isArabic) "حفظ وإدراج" else "LOG PATTERN", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text(if (isArabic) "تجاهل" else "CANCEL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF16161A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- DIALOG CONFIRMING ACTION OF REMOVING CUSTOM EXERCISE ---
    exerciseToDelete?.let { ex ->
        val dialogTitle = if (isArabic) "حذف التمرين المخصص؟" else "DELETE CUSTOM PATTERN?"
        val dialogMessage = if (isArabic) {
            "هل أنت متأكد من رغبتك في إزالة التمرين المخصص \"${ex.nameAr}\" نهائياً؟"
        } else {
            "Are you completely sure you want to delete \"${ex.nameEn}\" custom exercise?"
        }

        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = {
                Text(
                    text = dialogTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedList = customExercisesList.filterNot { it.id == ex.id }
                        saveCustomExercises(context, updatedList)
                        customExercisesList = updatedList
                        exerciseToDelete = null
                        activity.triggerVibration(120)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text(if (isArabic) "نعم، حذف تماماً" else "YES, DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text(if (isArabic) "إلغاء التراجع" else "CANCEL", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1F),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- DIALOG CONFIRMING RESET OF ALL WORKOUT PROGRESS ---
    if (showResetConfirmDialog) {
        val resetTitle = if (isArabic) "إعادة تعيين التقدم بالكامل؟" else "RESET ALL PROGRESS?"
        val resetMessage = if (isArabic) {
            "هل أنت كابتن واثق تماماً من رغبتك في مسح كافة أوزان وجولات وتواريخ التمارين اليومية؟ لا يمكن التراجع عن هذا الإجراء."
        } else {
            "Are you completely sure you want to hard reset all workout logs, weights, reps, and sets? This clean slate cannot be undone."
        }

        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = resetTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = resetMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWorkouts()
                        clearAllWeightsAndReps(context)
                        showResetConfirmDialog = false
                        activity.triggerVibration(150)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text(if (isArabic) "نعم، تصفير كلي" else "YES, RESET ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(if (isArabic) "تراجع" else "CANCEL", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1F),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ModernExerciseCard(
    exercise: ExerciseDef,
    progress: WorkoutProgress,
    isArabic: Boolean,
    context: Context,
    activity: MainActivity,
    onSetChecked: (Int, Boolean) -> Unit,
    onDeleteRequested: () -> Unit
) {
    val allDone = progress.set1Done && progress.set2Done && progress.set3Done
    val borderCol = if (allDone) NeonYellow.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
    var isExpanded by remember { mutableStateOf(false) }

    // Saved weights/reps inputs values
    var weightSet1 by remember { mutableStateOf(getSavedValue(context, exercise.id, 1, "weight", "60")) }
    var repsSet1 by remember { mutableStateOf(getSavedValue(context, exercise.id, 1, "reps", "10")) }
    
    var weightSet2 by remember { mutableStateOf(getSavedValue(context, exercise.id, 2, "weight", "60")) }
    var repsSet2 by remember { mutableStateOf(getSavedValue(context, exercise.id, 2, "reps", "10")) }

    var weightSet3 by remember { mutableStateOf(getSavedValue(context, exercise.id, 3, "weight", "60")) }
    var repsSet3 by remember { mutableStateOf(getSavedValue(context, exercise.id, 3, "reps", "10")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .iosSquircleCard(
                cornerSizeRatio = 0.28f,
                baseOpacity = if (allDone) 0.26f else 0.16f,
                saturationScale = 1.80f
            )
            .clickable { 
                isExpanded = !isExpanded 
                activity.triggerVibration(30)
            }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Elite left accent glowing bracket
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (allDone) NeonYellow else DimGray)
                )
    
                Spacer(modifier = Modifier.width(10.dp))
    
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) exercise.nameAr else exercise.nameEn,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Muscle targeted badge
                        Box(
                            modifier = Modifier
                                .background(NeonYellow.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isArabic) exercise.muscleAr else exercise.muscleEn).uppercase(),
                                color = NeonYellow,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Equipment badge
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isArabic) exercise.equipmentAr else exercise.equipmentEn).uppercase(),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Difficulty dot and badge
                        val diffColor = when(exercise.difficultyEn.lowercase()) {
                            "beginner" -> Color(0xFF4CAF50)
                            "intermediate" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(diffColor)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isArabic) exercise.difficultyAr else exercise.difficultyEn,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
    
                // Custom delete button if it is custom exercise
                if (exercise.id.startsWith("custom_")) {
                    IconButton(
                        onClick = { 
                            activity.triggerVibration(60)
                            onDeleteRequested() 
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete custom exercise",
                            tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Smooth expansion arrow indicator
                IconButton(
                    onClick = { 
                        isExpanded = !isExpanded 
                        activity.triggerVibration(30)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) PhosphorIcons.CaretUp else PhosphorIcons.CaretDown,
                        contentDescription = "Expand controls",
                        tint = if (allDone) NeonYellow else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
    
            Spacer(modifier = Modifier.height(14.dp))
    
            // Checklists sets tactile block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SetCheckToggle(label = if (isArabic) "مجموعة ١" else "Set 1", isChecked = progress.set1Done) { 
                    onSetChecked(1, !progress.set1Done) 
                }
                SetCheckToggle(label = if (isArabic) "مجموعة ٢" else "Set 2", isChecked = progress.set2Done) { 
                    onSetChecked(2, !progress.set2Done) 
                }
                SetCheckToggle(label = if (isArabic) "مجموعة ٣" else "Set 3", isChecked = progress.set3Done) { 
                    onSetChecked(3, !progress.set3Done) 
                }
            }
    
            // Expanding weight counter dashboard & Instructions guide
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f), SquircleShape(0.24f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), SquircleShape(0.24f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show Instructions Title and text
                    if (exercise.instructionsEn.isNotEmpty() || exercise.instructionsAr.isNotEmpty()) {
                        Column {
                            Text(
                                text = if (isArabic) "طريقة الأداء والتعليمات الفنية:" else "TECHNICAL EXECUTION GUIDELINE:",
                                color = NeonYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) exercise.instructionsAr else exercise.instructionsEn,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.White.copy(alpha = 0.06f))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Text(
                        text = if (isArabic) "مخطط الوزن والبطولات لهذه الجولة:" else "TACTICAL LOAD METRICS CONFIGURATION:",
                        color = NeonYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
    
                    SetDialTuner(
                        setTitle = if (isArabic) "الأولى" else "Set 1",
                        weight = weightSet1,
                        reps = repsSet1,
                        isArabic = isArabic,
                        activity = activity,
                        onWeightChange = {
                            weightSet1 = it
                            saveValue(context, exercise.id, 1, "weight", it)
                        },
                        onRepsChange = {
                            repsSet1 = it
                            saveValue(context, exercise.id, 1, "reps", it)
                        }
                    )
    
                    SetDialTuner(
                        setTitle = if (isArabic) "الثانية" else "Set 2",
                        weight = weightSet2,
                        reps = repsSet2,
                        isArabic = isArabic,
                        activity = activity,
                        onWeightChange = {
                            weightSet2 = it
                            saveValue(context, exercise.id, 2, "weight", it)
                        },
                        onRepsChange = {
                            repsSet2 = it
                            saveValue(context, exercise.id, 2, "reps", it)
                        }
                    )
    
                    SetDialTuner(
                        setTitle = if (isArabic) "الثالثة" else "Set 3",
                        weight = weightSet3,
                        reps = repsSet3,
                        isArabic = isArabic,
                        activity = activity,
                        onWeightChange = {
                            weightSet3 = it
                            saveValue(context, exercise.id, 3, "weight", it)
                        },
                        onRepsChange = {
                            repsSet3 = it
                            saveValue(context, exercise.id, 3, "reps", it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SetCheckToggle(label: String, isChecked: Boolean, onClick: () -> Unit) {
    val activeBg = if (isChecked) NeonYellow.copy(alpha = 0.15f) else Color(0x1AFFFFFF)
    val activeBorder = if (isChecked) NeonYellow else Color.White.copy(alpha = 0.08f)
    val textCol = if (isChecked) Color.White else Color.White.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = PhosphorIcons.Check, 
                    contentDescription = null, 
                    tint = NeonYellow, 
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = textCol,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

@Composable
fun SetDialTuner(
    setTitle: String,
    weight: String,
    reps: String,
    isArabic: Boolean,
    activity: MainActivity,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setTitle, 
            color = Color.White, 
            fontWeight = FontWeight.Black, 
            fontSize = 12.sp
        )
 
        // Kilograms load editor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isArabic) "وزن: " else "W: ",
                color = DimGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { 
                    val w = weight.toIntOrNull() ?: 60
                    onWeightChange(maxOf(0, w - 5).toString())
                    activity.triggerVibration(25)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Remove, 
                    contentDescription = "decrease weight", 
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), SquircleShape(0.48f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${weight}kg", 
                    color = NeonYellow, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            IconButton(
                onClick = { 
                    val w = weight.toIntOrNull() ?: 60
                    onWeightChange((w + 5).toString())
                    activity.triggerVibration(25)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Add, 
                    contentDescription = "increase weight", 
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
 
        // Repetitions counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isArabic) "تكرارات: " else "R: ",
                color = DimGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { 
                    val r = reps.toIntOrNull() ?: 10
                    onRepsChange(maxOf(1, r - 1).toString())
                    activity.triggerVibration(20)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Remove, 
                    contentDescription = "decrease reps", 
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), SquircleShape(0.48f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = reps, 
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = { 
                    val r = reps.toIntOrNull() ?: 10
                    onRepsChange((r + 1).toString())
                    activity.triggerVibration(20)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Add, 
                    contentDescription = "increase reps", 
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun CircularRestTimerModule(
    secondsLeft: Int,
    isRunning: Boolean,
    isArabic: Boolean,
    onPresetSelect: (Int) -> Unit,
    onAdjustTimer: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonYellow.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = (if (isArabic) "مؤشر الراحة النشط" else "CYBERNETIC INSTANT RECOVERY HUD").uppercase(),
                color = NeonYellow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))
 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clock numerical digital display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isArabic) "زمن الاستشفاء" else "COCONUT REST",
                        color = DimGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
 
                // Controls and quick-select presets
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 45, 60, 90).forEach { s ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { onPresetSelect(s) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${s}s", 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onAdjustTimer(-15) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Remove, 
                                contentDescription = "-15s", 
                                tint = Color.White, 
                                modifier = Modifier.size(12.dp)
                            )
                        }
 
                        Text(
                            text = if (isArabic) "تعديل الراحة" else "ADJUST",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
 
                        IconButton(
                            onClick = { onAdjustTimer(15) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Add, 
                                contentDescription = "+15s", 
                                tint = Color.White, 
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
 
            Spacer(modifier = Modifier.height(12.dp))
 
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play-pause key
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) PhosphorIcons.Pause else PhosphorIcons.Play,
                        contentDescription = "play timer",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) (if (isArabic) "إيقاف مؤقت" else "PAUSE") else (if (isArabic) "تشغيل" else "START"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
 
                // Skip Rest Key
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.SkipNext,
                        contentDescription = "Skip rest",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "تخطي الراحة" else "SKIP REST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Global Preferences Helpers
fun loadCustomExercises(context: Context): List<CustomExercise> {
    val prefs = context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("custom_exercises_list", "") ?: ""
    if (jsonStr.isEmpty()) return emptyList()
    return try {
        Json.decodeFromString<List<CustomExercise>>(jsonStr)
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveCustomExercises(context: Context, list: List<CustomExercise>) {
    val prefs = context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE)
    try {
        val jsonStr = Json.encodeToString(list)
        prefs.edit().putString("custom_exercises_list", jsonStr).apply()
    } catch (e: Exception) {
        // Safe fail
    }
}

fun saveValue(context: Context, exId: String, setIndex: Int, field: String, value: String) {
    val prefs = context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("ex_${exId}_set${setIndex}_$field", value).apply()
}

fun getSavedValue(context: Context, exId: String, setIndex: Int, field: String, default: String): String {
    val prefs = context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE)
    return prefs.getString("ex_${exId}_set${setIndex}_$field", default) ?: default
}

fun clearAllWeightsAndReps(context: Context) {
    val prefs = context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val allKeys = prefs.all
    for (entry in allKeys.entries) {
        if (entry.key.startsWith("ex_") && !entry.key.contains("custom_exercises_list")) {
            editor.remove(entry.key)
        }
    }
    editor.apply()
}
