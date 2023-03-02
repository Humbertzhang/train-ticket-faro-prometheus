function faro_init() {
    var dic = {
        app: {
            name: 'test',
        },
        transports: [
            new window.GrafanaFaroWebSdk.FetchTransport({
                url: 'http://47.103.205.96:19090/collect',
            }),
            new window.GrafanaFaroWebSdk.ConsoleTransport(),
        ],
        session: {
            id: Math.random().toString(36).slice(-8)
        },
        // instrumentations: [
        //     new ErrorsInstrumentation(),
        //     // new WebVitalsInstrumentation(),
        // ]
    }
    if (sessionStorage.getItem("session_id") === null) {
        sessionStorage.setItem("session_id", dic.session.id)
    } else {
        dic.session.id = sessionStorage.getItem("session_id")
    }
    window.GrafanaFaroWebSdk.initializeFaro(dic);
};

// Dynamically add the tracing instrumentation when the tracing bundle loads
function faro_addTracing() {
    window.GrafanaFaroWebSdk.faro.instrumentations.add(new window.GrafanaFaroWebTracing.TracingInstrumentation());
    // window.GrafanaFaroWebSdk.faro.instrumentations.add(new window.GrafanaFaroWebTracing.ErrorsInstrumentation());
};