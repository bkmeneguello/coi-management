"use strict";

COI.module("Laudo", function(Module, COI, Backbone, Marionette, $, _) {

	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});
	
	var Laudo = Backbone.Model.extend({
		urlRoot: '/rest/laudos',
		defaults: function() {
			return {
				data: new Date(),
				status: null,
				paciente: new Pessoa(),
				medico: new Pessoa(),
				dataNascimento: null,
				sexo: null,
				colunaLombarT1: true,
				colunaLombarT2: true,
				colunaLombarT3: true,
				colunaLombarT4: true,
				colunaLombarDensidade: null,
				colunaLombarTScore: null,
				colunaLombarZScore: null,
				coloFemurDensidade: null,
				coloFemurTScore: null,
				coloFemurZScore: null,
				femurTotalDensidade: null,
				femurTotalTScore: null,
				femurTotalZScore: null,
				radioTercoDensidade: null,
				radioTercoTScore: null,
				radioTercoZScore: null,
				corpoInteiroDensidade: null,
				corpoInteiroZScore: null
			};
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.parseDate('yy-mm-dd', resp.data);
			resp.paciente = new Pessoa(resp.paciente, {parse: true});
			resp.medico = new Pessoa(resp.medico, {parse: true});
			resp.dataNascimento = resp.dataNascimento ? $.datepicker.parseDate('yy-mm-dd', resp.dataNascimento) : null;
			return resp;
		}
	});
	
	var ExamePreMenopausaView = Marionette.ItemView.extend({
		template: '#exame_pre_menopausa_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['colunaLombarDensidade'].converter = decimalConverter;
			bindings['colunaLombarTScore'].converter = decimalConverter;
			bindings['colunaLombarZScore'].converter = decimalConverter;
			bindings['coloFemurDensidade'].converter = decimalConverter;
			bindings['coloFemurTScore'].converter = decimalConverter;
			bindings['coloFemurZScore'].converter = decimalConverter;
			bindings['femurTotalDensidade'].converter = decimalConverter;
			bindings['femurTotalTScore'].converter = decimalConverter;
			bindings['femurTotalZScore'].converter = decimalConverter;
			bindings['radioTercoDensidade'].converter = decimalConverter;
			bindings['radioTercoTScore'].converter = decimalConverter;
			bindings['radioTercoZScore'].converter = decimalConverter;
			//bindings['corpoInteiroDensidade'].converter = decimalConverter;
			//bindings['corpoInteiroZScore'].converter = decimalConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.$('input[type=text]').input();
		}
	});
	
	var LaudoView = COI.FormView.extend({
		template: '#laudo_template',
		regions: {
			'paciente': '#paciente',
			'medico': '#medico',
			'exame': '#exame'
		},
		modelEvents: {
			'change:paciente': 'renderPaciente',
			'change:medico': 'renderMedico',
			'change': 'renderExame'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			bindings['dataNascimento'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.renderPaciente();
			this.renderMedico();
			this.renderExame();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderPaciente: function() {
			this.paciente.show(new COI.PessoaView({model: this.model.get('paciente'), label: 'Paciente:', attribute: 'paciente', required: true}));
		},
		renderMedico: function() {
			this.medico.show(new COI.PessoaView({model: this.model.get('medico'), label: 'Médico:', attribute: 'cliente', required: true}));
		},
		renderExame: function() {
			this.exame.show(new ExamePreMenopausaView({model: this.model}));
		},
		onCancel: function(e) {
			Backbone.history.navigate('laudos', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('laudos', true);
			_notifySuccess();
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('laudo', function() {
			COI.body.show(new LaudoView({model: new Laudo()}));
		});
		COI.router.route('laudo(/:id)', function(id) {
			COI.body.show(new LaudoView({model: new Laudo({id: id})}));
		});
	});
});