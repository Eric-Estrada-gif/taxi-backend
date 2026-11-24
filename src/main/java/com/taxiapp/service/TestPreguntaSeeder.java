package com.taxiapp.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestPreguntaSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbc;

	public TestPreguntaSeeder(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		Integer n;
		try {
			n = jdbc.queryForObject("SELECT COUNT(*) FROM test_pregunta", Integer.class);
		} catch (Exception e) {
			return;
		}
		if (n != null && n > 0) {
			return;
		}
		for (String[] p : CONDUCTOR) {
			insertar("CONDUCTOR", p);
		}
		for (String[] p : RIDER) {
			insertar("RIDER", p);
		}
	}

	private void insertar(String rol, String[] p) {
		jdbc.update(
				"INSERT INTO test_pregunta (rol, enunciado, opcion_a, opcion_b, opcion_c, opcion_d, correcta) VALUES (?,?,?,?,?,?,?)",
				rol, p[0], p[1], p[2], p[3], p[4], p[5]);
	}

	private static final String[][] CONDUCTOR = {
			{"Antes de ponerte en linea tomaste dos cervezas. Que haces?",
					"Salgo igual, total son pocas", "Espero estar sobrio o no trabajo hoy", "Tomo cafe y salgo", "Acepto solo viajes cortos", "B"},
			{"Llevas 12 horas al volante y te sientes agotado.",
					"Sigo por la propina", "Me desconecto y descanso", "Tomo una bebida energetica y sigo", "Pongo musica fuerte", "B"},
			{"Un pasajero te escribe mensajes incomodos en el chat.",
					"Le respondo igual", "Ignoro y continuo como si nada grave", "Cancelo, reporto y priorizo mi seguridad y la del viaje", "Publico su numero", "C"},
			{"El pasajero discute la tarifa al bajar.",
					"Lo encierro hasta que pague de mas", "Grito y lo bajo a la fuerza", "Mantengo la calma, muestro el monto de la app y si no hay acuerdo uso el soporte", "Lo persigo a pie", "C"},
			{"El rider cancela cuando ya estas a una cuadra.",
					"Lo insulto por chat", "Acepto la regla de la app y sigo disponible", "Voy a su casa a reclamar", "Le pongo 1 estrella siempre", "B"},
			{"Un amigo quiere subirse sin estar en el viaje.",
					"Lo subo, nadie se entera", "No: solo viaja quien pidio el taxi", "Solo si es de noche", "Si el pasajero no se queja", "B"},
			{"Quieres grabar al pasajero sin avisarle.",
					"Esta bien por seguridad mia", "Solo audio", "No, no grabo ni publico sin consentimiento", "Lo subo a redes", "C"},
			{"Hay un accidente leve sin heridos.",
					"Me voy rapido", "Me detengo, auxilio si hace falta y aviso por la app / autoridades", "Culpo al pasajero", "Niego que yo manejaba", "B"},
			{"El GPS te pide un sentido contrario ilegal.",
					"Lo hago para llegar antes", "Respeto las normas aunque tarde un poco mas", "Apago luces y paso", "Pido que el pasajero mire", "B"},
			{"El pasajero pide una parada no pactada en zona insegura para ti.",
					"Acepto siempre", "Evaluo, explico y si no es seguro propongo un punto visible o cancelo con motivo", "Lo dejo en cualquier sitio sin avisar", "Me enojo y lo bajo", "B"},
			{"Vas usando el celular en la mano mientras manejas.",
					"Es normal", "Solo WhatsApp", "No: uso soporte / voz y priorizo el volante", "Si no hay policia", "C"},
			{"Una pasajera viaja sola de noche. Como te comportas?",
					"Hago preguntas personales", "Viaje profesional, ruta clara, sin comentarios invasivos", "Le pido el Instagram", "Cambio de ruta por las suyas", "B"},
			{"El pasajero se olvido una mochila.",
					"Me la quedo", "La reporto en la app y sigo el protocolo de objetos perdidos", "La abro para ver que hay", "La dejo en la vereda", "B"},
			{"Te piden ir mas rapido saltandote un rojo.",
					"Acelero si pagan extra", "No: no pongo en riesgo a nadie", "Solo de madrugada", "Si no hay camaras", "B"},
			{"Otro conductor te cierra. Reaccionas:",
					"Lo persigo y lo encaro", "Respiro, no escalo y sigo el viaje", "Toco claxon 2 minutos", "Filmo para pelear en redes", "B"},
			{"El SOAT de tu auto vencio ayer.",
					"Salgo igual un dia", "No me pongo en linea hasta renovarlo", "Es problema del pasajero", "Lo cubre la app", "B"},
			{"El pasajero esta claramente alcoholizado y agresivo.",
					"Sigo el viaje si ya acepte", "Priorizo seguridad: no inicio o detengo en lugar seguro y reporto", "Le pego", "Lo dejo en la pista", "B"},
			{"Te equivocaste de ruta y alargas el viaje.",
					"Niego el error", "Aviso, corrijo y no presiono para extra", "Cobro de mas en efectivo", "Insisto que el GPS nunca falla", "B"},
			{"Un menor quiere viajar solo de madrugada y te da mala espina.",
					"Lo llevo sin preguntar", "Sigo politicas: no transporte inseguro / reporto si hay riesgo", "Le pido dinero extra", "Lo dejo lejos a proposito", "B"},
			{"Terminaste el viaje. El pasajero te califica mal injustamente.",
					"Lo busco para pelear", "Sigo, uso reclamo formal si aplica, no lo hostigo", "Le mando mensajes personales", "Le pongo 1 a todos sus amigos", "B"},
			{"Hay niebla y poca visibilidad.",
					"Voy igual de rapido", "Reduzco velocidad y uso luces adecuadas", "Apago luces para no cansar", "Cierro los ojos un segundo", "B"},
			{"El pasajero no usa cinturon.",
					"No es mi problema", "Le pido que se lo ponga antes de salir", "Arranco igual", "Me burlo", "B"},
			{"Te ofrecen llevar un paquete cerrado de un desconocido.",
					"Lo llevo por extra", "No transporto encargos dudosos", "Solo si pesa poco", "Lo abro en el semaforo", "B"},
			{"La app se cayo en medio del viaje.",
					"Abandono al pasajero", "Completo el servicio de forma segura y regularizo luego", "Pido el doble en efectivo", "Lo dejo a mitad de camino", "B"},
			{"Un pasajero tose muy enfermo y te pide cerrar ventanas.",
					"Me enojo y lo bajo ya", "Acuerdo ventilacion y mascarilla; si no hay respeto, cancelo con motivo", "Sigo sin decir nada aunque temas", "Lo grabo para exponerlo", "B"},
			{"Ves un retén o control policial.",
					"Doy la vuelta para evitarlo", "Me detengo y muestro documentos", "Pido al pasajero que mienta", "Acelero", "B"},
			{"El destino es un callejón oscuro y te sientes en riesgo.",
					"Entro si o si", "Dejo en un punto iluminado y explico", "Dejo al pasajero y me voy sin decir", "Apago el auto y me bajo a discutir", "B"},
			{"El pasajero quiere pagar menos en efectivo.",
					"Acepto para no pelear siempre", "Mantengo el monto de la app o uso soporte; no amenazo", "Lo encierro", "Le saco el celular", "B"},
			{"Estas resfriado y con sueno.",
					"Igual hago turnos largos", "No manejo si no estoy apto", "Me automedico y salgo", "Tomo bebidas para aguantar", "B"},
			{"Una compañera conductora te cuenta acoso de un usuario.",
					"Es chisme, no hago nada", "La apoyo a reportar por el canal de la app", "Le digo que se aguante", "Publico el nombre del usuario", "B"},
			{"El mapa te manda por una via exclusiva prohibida.",
					"La uso para ganar tiempo", "Busco via legal", "Si nadie me ve, paso", "El pasajero decide infracciones", "B"},
			{"El pasajero pide musica a volumen ensordecedor.",
					"Pongo lo que quiera el siempre", "Acuerdo un volumen seguro para manejar", "Me peleo", "Apago el auto", "B"},
			{"Terminando, el pasajero te pide tu numero personal.",
					"Se lo doy siempre", "Mantengo el contacto por la app", "Le escribo luego yo", "Lo agrego a WhatsApp grupal", "B"},
			{"Hay un nino en el asiento sin silla adecuada.",
					"Es culpa del padre, arranco", "No inicio si hay riesgo evidente; explico", "Voy mas lento y ya", "Lo cargo adelante", "B"},
			{"Te piden que esperes 20 minutos con el taximetro en un sitio.",
					"Me voy sin avisar", "Aviso el tiempo de espera de la app y actuo segun reglas", "Cobro extra informal", "Me duermo al volante", "B"},
			{"Otro socio te pide tu cuenta para manejar el.",
					"Se la presto un rato", "No: la cuenta es personal y de seguridad", "Si me paga", "Solo fines de semana", "B"},
			{"El pasajero filma el interior sin pedir permiso.",
					"Le arrebato el celular", "Pido que no grabe y si insiste reporto / detengo de forma segura", "Me pongo a gritar", "Grabo yo tambien a escondidas", "B"},
			{"Detectas que el destino coincide con una zona de riesgo para ti de noche.",
					"Acepto sin pensar", "Puedo rechazar por seguridad segun politica, con respeto", "Acepto y luego abandono", "Insulto al rider", "B"},
			{"El aceite o frenos del auto fallan.",
					"Hago un viaje mas", "No salgo hasta que el auto este seguro", "Es problema de mecanica luego", "Aviso al pasajero que se agarré", "B"},
			{"Un pasajero te discrimina.",
					"Respondo igual", "No escalo, documento y reporto; cancelo si hay riesgo", "Lo bajo a golpes", "Acepto todo callado aunque haya amenaza", "B"}
	};

	private static final String[][] RIDER = {
			{"El conductor te pide que uses cinturon.",
					"No es obligatorio en taxi", "Me lo pongo", "Me rio", "Abro la puerta en marcha", "B"},
			{"No estas de acuerdo con el precio al final.",
					"Me bajo sin pagar", "Reviso la app y uso soporte, sin agredir", "Amenazo al conductor", "Le saco foto para exponerlo", "B"},
			{"Vas muy apurado. El semaforo esta en rojo.",
					"Le pido que se lo pase", "Espero; no pido infracciones", "Ofrezco extra por correr", "Grito", "B"},
			{"Cancelas el viaje cuando el auto ya llego.",
					"Lo hago seguido, no pasa nada", "Solo si hay motivo real; se que afecta al conductor", "Me burlo por chat", "Pido otro inmediatamente para molestar", "B"},
			{"El conductor es de otro distrito o acento.",
					"Lo trato peor", "Mismo respeto que a cualquier persona", "Hago chistes de su origen", "Pido cambiar de conductor por eso", "B"},
			{"Olvidaste una mochila.",
					"Acuso de robo en redes al toque", "Aviso por la app y espero el protocolo", "Voy a su casa a pelear", "Pongo 1 estrella ya", "B"},
			{"El chat del conductor es solo coordinacion del viaje.",
					"Le pido su WhatsApp personal si o si", "Respeto el canal de la app", "Le mando audios de madrugada", "Lo insulto si tarda 1 min", "C"},
			{"El auto huele a desinfectante y no te gusta.",
					"Insulto", "Comento con respeto o califico al final sin agredir", "Abro todo y tiro cosas", "Me bajo en movimiento", "B"},
			{"Quieres parar en un sitio no pactado y oscuro.",
					"Exijo que entre", "Pido un punto seguro de comun acuerdo", "Amenazo con no pagar", "Abro la puerta", "B"},
			{"Vas alcoholizado y el conductor pide calma.",
					"Me pongo agresivo", "Respeto; si no estoy apto cancelo", "Le pego al asiento", "Le escupo", "B"},
			{"El conductor se equivoco de calle.",
					"Lo grabo para humillarlo", "Aviso con calma para corregir", "No pago nada", "Lo denuncio falso", "B"},
			{"Hay un menor contigo sin silla.",
					"Es problema del taxista", "No viajamos si no es seguro", "Lo cargo en brazos adelante", "Le pido que acelere", "B"},
			{"El conductor no acepta una parada extra no pagada.",
					"Me enojo y no pago el viaje", "Entiendo que debe estar en la app", "Lo amenazo", "Le bajo la calificacion por venganza siempre", "B"},
			{"Llegas y no encuentras efectivo.",
					"Me bajo corriendo", "Aviso, uso el metodo de la app o acuerdo legal", "Culpo al conductor", "Le dejo un objeto de valor a la fuerza", "B"},
			{"El conductor pone musica suave.",
					"Exijo regueton al maximo", "Pido un volumen razonable", "Toco yo la radio a escondidas", "Le grito", "B"},
			{"Vas a calificar 1 estrella porque tuviste un mal dia, no por el viaje.",
					"Lo hago igual", "Califico el servicio real, no mi humor", "Ademas lo insulto en el comentario", "Pido banearlo", "B"},
			{"El conductor te pide no comer en el auto.",
					"Como igual", "Respeto su auto", "Dejo las cascaras", "Me burlo", "B"},
			{"Hay un retén. El conductor se detiene.",
					"Le digo que se fugue", "Cooperamos con las autoridades", "Escondo cosas ilegales", "Me bajo y huyo", "B"},
			{"El viaje es de noche y vas sola/o. El conductor es profesional.",
					"Aun asi lo grabo oculto siempre", "Puedo compartir mi viaje con un contacto de confianza por la app", "Le pido que me acompañe al departamento", "Le doy mi direccion exacta interna", "B"},
			{"El conductor no habla mucho.",
					"Lo obligo a conversar", "Respeto el silencio", "Me ofendo y califico mal", "Le tomo fotos", "B"},
			{"Quieres llevar a un amigo que no esta en el viaje.",
					"Lo subo igual", "No, o actualizo el viaje si la app lo permite", "Escondo a la persona", "Pago extra en negro", "B"},
			{"El conductor comete una falta de transito.",
					"Lo grabo y lo chantajeo", "Si es grave, reporto por el canal oficial", "Lo golpea", "Grito todo el viaje", "B"},
			{"Llegas tarde a una cita. Culpas al taxi.",
					"Lo maltrato", "Salgo con tiempo la proxima; trato correcto", "No pago", "Digo que me secuestro", "B"},
			{"El auto tiene olor a cigarro residual.",
					"Quemo algo para tapar", "Comento educado o elijo no viajar", "Rompo algo", "Escupo", "B"},
			{"Te pide confirmar el pin de la app.",
					"Se lo niego para no pagar", "Cooperas con la seguridad del viaje", "Inventas un pin", "Te burlas", "B"},
			{"El conductor es mujer. Cambias tu trato?",
					"Si, soy mas grosero", "No: mismo respeto", "Le pido el numero", "Dudo de su manejo por ser mujer", "B"},
			{"Hay trafico. El conductor elige otra via legal mas larga.",
					"Lo acuso de alargar a proposito sin prueba", "Pregunto con calma y confio si es razonable", "Me bajo en la via", "No pago", "B"},
			{"Recibes un objeto que no es tuyo en el asiento.",
					"Me lo llevo", "Lo aviso de inmediato", "Lo tiro", "Lo vendo", "B"},
			{"El conductor te dice que no puede entrar a una zona prohibida.",
					"Insisto en que infraccione", "Bajo en un punto permitido", "Lo amenazo con denuncia falsa", "Cierro el seguro", "B"},
			{"Estas enfermo y toses sin taparte.",
					"Es mi derecho", "Uso mascarilla y aviso; cuido al otro", "Me acerco mas", "Niego sintomas", "B"},
			{"El conductor pide que no golpes las puertas.",
					"Las azoto mas", "Cuido el vehiculo", "Me rio", "Dejo el cinturon atascado a proposito", "B"},
			{"Quieres dar propina.",
					"Es obligatorio gritarlo", "Si quiero, por el medio de la app o acuerdo claro", "La uso para exigir favores", "Digo que di y no di", "B"},
			{"El conductor se pierde y pide ayuda.",
					"Lo humillo", "Indico con respeto", "No hablo y luego 1 estrella", "Abro la puerta en marcha", "B"},
			{"Hay un bebe y el conductor pide ir despacio.",
					"Le exijo velocidad", "Acepto ir seguro", "Me quejo todo el viaje", "Digo que pierdo el tiempo", "B"},
			{"Te escriben por error a otro viaje.",
					"Insulto", "Aviso el error y corto", "Sigo la conversacion personal", "Comparto datos de terceros", "B"},
			{"El conductor no quiere conversar de politica.",
					"Lo obligo", "Cambio de tema o silencio", "Lo grabo", "Califico 1 por eso", "B"},
			{"Al bajar dejas basura.",
					"El auto es publico", "No dejo basura", "La escondo bajo el asiento", "Digo que ya estaba", "B"},
			{"El conductor cancela por seguridad (zona de riesgo).",
					"Lo acoso", "Entiendo y pido otro viaje", "Voy a buscarlo", "Publico su placa con odio", "B"},
			{"Quieres pagar con un metodo distinto al de la app a ultimo minuto.",
					"Exijo y peleo", "Si no se puede, uso el pactado o cancelo a tiempo", "Me bajo sin pagar", "Le dejo un vale de tienda", "B"},
			{"Ves que el conductor usa el celular al manejar.",
					"Le gritas insultos", "Pides con calma que se enfoque; si hay riesgo, reportas", "Filmas para fama", "Le quitas el celular", "B"}
	};
}
