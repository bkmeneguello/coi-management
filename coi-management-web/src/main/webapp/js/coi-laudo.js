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
				dataNascimento: null
			};
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.formatDate('dd/mm/yy', $.datepicker.parseDate('yy-mm-dd', resp.data));
			resp.paciente = new Pessoa(resp.paciente, {parse: true});
			resp.medico = new Pessoa(resp.medico, {parse: true});
			return resp;
		}
	});
	
	var LaudoView = COI.FormView.extend({
		template: '#laudo_template',
		regions: {
			'paciente': '#paciente',
			'medico': '#medico'
		},
		modelEvents: {
			'change:paciente': 'renderPaciente',
			'change:medico': 'renderMedico',
		},
		initialize: function() {
			
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.renderPaciente();
			this.renderMedico();
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