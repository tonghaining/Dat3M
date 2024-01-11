lexer grammar OpenCLLexer;

OpenCLWG                      :   'WG';
OpenCLDEV                     :   'DEV';

OpenCLAtomicFenceWI           :   'atomic_work_item_fence';

OpenCLMemoryScopeWI           :   'memory_scope_work_item';
OpenCLMemoryScopeWG           :   'memory_scope_work_group';
OpenCLMemoryScopeDEV          :   'memory_scope_device';
OpenCLMemoryScopeALL          :   'memory_scope_all_svm_devices';

OpenCLFenceFlagGL             :   'CLK_GLOBAL_MEM_FENCE';
OpenCLFenceFlagLC             :   'CLK_LOCAL_MEM_FENCE';
OpenCLFenceFlagIMG            :   'CLK_IMAGE_MEM_FENCE';
