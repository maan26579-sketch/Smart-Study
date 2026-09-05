package com.example.data.model

data class StudySample(
    val title: String,
    val subject: String,
    val type: String, // WHITEBOARD_SCAN or AUDIO_LECTURE
    val content: String,
    val description: String
)

object SampleMaterials {
    val samples = listOf(
        StudySample(
            title = "Work-Energy Theorem & Conservative Fields",
            subject = "Physics",
            type = "WHITEBOARD_SCAN",
            description = "Whiteboard with free-body diagrams, path integrals, and conservative force proofs.",
            content = """
                [Board 1: Hall C - Mechanics Lecture]
                Definition of Work: W = integral F dot dr along path C.
                Conservative Force definition: Curl of F = 0 <=> Work is path-independent <=> F = -nabla U.
                Work-Energy Theorem: Total work W_net = Delta K = (1/2) m (v_f^2 - v_i^2).
                If only conservative forces act: Delta K + Delta U = 0, so Mechanical Energy E = K + U = Constant.
                Non-conservative work (Friction/Drag): W_nc = Delta E_mech = E_final - E_initial < 0.
                Example: Block sliding down rough incline of angle theta with friction coefficient mu_k.
                Normal Force: N = m * g * cos(theta)
                Friction Force: f_k = mu_k * N = mu_k * m * g * cos(theta)
                Work done by friction over distance L: W_friction = - f_k * L.
                Kinetic energy at bottom: K_bottom = m * g * L * sin(theta) - mu_k * m * g * L * cos(theta).
            """.trimIndent()
        ),
        StudySample(
            title = "Cellular Respiration & Chemiosmosis",
            subject = "Biology",
            type = "WHITEBOARD_SCAN",
            description = "Whiteboard diagram tracing glycolysis, Krebs cycle, and mitochondrial ETC complexes.",
            content = """
                [Board 3: Bio 102 - Bioenergetics]
                Overall Respiration Equation: C6H12O6 + 6 O2 -> 6 CO2 + 6 H2O + ~30-32 ATP.
                Stage 1: Glycolysis in Cytoplasm. Glucose (6C) -> 2 Pyruvate (3C) + 2 net ATP (substrate-level) + 2 NADH.
                Stage 2: Pyruvate Oxidation into Acetyl-CoA + 1 CO2 per pyruvate.
                Stage 3: Citric Acid / Krebs Cycle in Mitochondrial Matrix. Produces 6 NADH, 2 FADH2, 2 ATP/GTP, 4 CO2 per glucose.
                Stage 4: Oxidative Phosphorylation on Inner Mitochondrial Membrane.
                Complexes I, III, IV pump H+ protons from matrix into intermembrane space, building electrochemical proton gradient (pmf - proton motive force).
                Terminal electron acceptor is O2, forming H2O: 1/2 O2 + 2 H+ + 2 e- -> H2O.
                Complex V: ATP Synthase rotary motor driven by H+ flux down electrochemical gradient back into matrix, synthesizing ATP from ADP + Pi.
            """.trimIndent()
        ),
        StudySample(
            title = "Central Bank Monetary Policy & Inflation",
            subject = "Economics",
            type = "AUDIO_LECTURE",
            description = "Audio lecture transcription on dual mandates, Taylor Rule, and Quantitative Easing.",
            content = """
                Prof. Henderson: 'Good morning everyone. Today let's discuss Monetary Transmission Mechanisms.
                The Federal Reserve operates under a dual mandate: maximum sustainable employment and price stability, pegged at a 2 percent inflation target.
                When headline inflation accelerates above target, the Federal Open Market Committee raises the policy rate (the Federal Funds rate).
                How does this translate to real economic activity?
                First, higher policy rates increase borrowing costs across prime bank lending, mortgages, and corporate bonds.
                Second, higher borrowing costs dampen interest-sensitive aggregate demand, notably capital expenditures and consumer durables.
                Third, according to the Taylor Rule: Target Rate equals Neutral Rate plus 0.5 times the Inflation Gap plus 0.5 times the Output Gap.
                When central banks hit the Zero Lower Bound, conventional interest rate cuts lose efficacy.
                This triggers unconventional monetary policies: Quantitative Easing (large-scale asset purchases) to compress term premiums along the long end of the yield curve.'
            """.trimIndent()
        ),
        StudySample(
            title = "Graph Algorithms: BFS vs DFS & Dijkstra",
            subject = "Computer Science",
            type = "AUDIO_LECTURE",
            description = "Discussion on queue vs stack traversal, priority queues, and negative cycle constraints.",
            content = """
                Lecturer: 'Let's compare fundamental graph traversal paradigms: Breadth-First Search versus Depth-First Search.
                BFS utilizes a First-In-First-Out Queue. It explores neighbors layer by layer in concentric radial rings.
                Crucially, in unweighted graphs, BFS guarantees discovery of the shortest path in O(V + E) time.
                DFS utilizes a Last-In-First-Out Stack, often implemented recursively. DFS is ideal for topological sorting, cycle detection via back-edges, and finding strongly connected components.
                When edge weights are introduced, BFS fails to find shortest paths. Here we transition to Dijkstra's Algorithm.
                Dijkstra maintains a min-priority queue of tentative distances. At each step, it greedily extracts the vertex with minimum tentative distance and relaxes its outgoing edges.
                Time complexity with a binary min-heap is O((V + E) log V).
                Critical limitation: Dijkstra fails if any negative edge weight exists because the greedy finality assumption is violated. For negative edges, we must use Bellman-Ford in O(V * E).'
            """.trimIndent()
        )
    )
}
