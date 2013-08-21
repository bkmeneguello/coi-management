"use strict";

COI.module("Laudo", function(Module, COI, Backbone, Marionette, $, _) {

	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});
	
	var Observacao = Backbone.Model.extend({
		defaults: {
			codigo: null,
			descricao: null,
			extra: null
		}
	});
	
	var Observacoes = Backbone.Collection.extend({
		model: Observacao
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
				colunaLombarL1: true,
				colunaLombarL2: true,
				colunaLombarL3: true,
				colunaLombarL4: true,
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
				corpoInteiroZScore: null,
				conclusao: null,
				observacoes: new Observacoes()
			};
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.parseDate('yy-mm-dd', resp.data);
			resp.paciente = new Pessoa(resp.paciente, {parse: true});
			resp.medico = new Pessoa(resp.medico, {parse: true});
			resp.dataNascimento = resp.dataNascimento ? $.datepicker.parseDate('yy-mm-dd', resp.dataNascimento) : null;
			resp.observacoes = new Observacoes(resp.observacoes, {parse: true});
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
			bindings['colunaLombarDensidade'].converter = decimal3Converter;
			bindings['colunaLombarTScore'].converter = decimalConverter;
			bindings['colunaLombarZScore'].converter = decimalConverter;
			bindings['coloFemurDensidade'].converter = decimal3Converter;
			bindings['coloFemurTScore'].converter = decimalConverter;
			bindings['coloFemurZScore'].converter = decimalConverter;
			bindings['femurTotalDensidade'].converter = decimal3Converter;
			bindings['femurTotalTScore'].converter = decimalConverter;
			bindings['femurTotalZScore'].converter = decimalConverter;
			bindings['radioTercoDensidade'].converter = decimal3Converter;
			bindings['radioTercoTScore'].converter = decimalConverter;
			bindings['radioTercoZScore'].converter = decimalConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.$('input[type=text]').input();
		}
	});
	
	var ExamePosMenopausaView = Marionette.ItemView.extend({
		template: '#exame_pos_menopausa_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['colunaLombarDensidade'].converter = decimal3Converter;
			bindings['colunaLombarZScore'].converter = decimalConverter;
			bindings['coloFemurDensidade'].converter = decimal3Converter;
			bindings['coloFemurZScore'].converter = decimalConverter;
			bindings['femurTotalDensidade'].converter = decimal3Converter;
			bindings['femurTotalZScore'].converter = decimalConverter;
			bindings['radioTercoDensidade'].converter = decimal3Converter;
			bindings['radioTercoZScore'].converter = decimalConverter;
			bindings['corpoInteiroDensidade'].converter = decimalConverter;
			bindings['corpoInteiroZScore'].converter = decimalConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.$('input[type=text]').input();
		}
	});
	
	var ObservacaoView = Backbone.View.extend({
		tagName: 'tr',
		className: 'coi-cell-item',
		render: function() {
			var that = this;
			this.$el.append($('<td>' + this.model.get('codigo') + '</td>'));
			this.$el.append($('<td>' + this.model.get('descricao').replace('{}', '<input type="text" name="extra"/>') + '</td>'));
			this.$el.append($('<td/>').append($('<button/>', {text: 'Remover'}).click(function(e) {
				if (e && e.preventDefault){ e.preventDefault(); }
		        if (e && e.stopPropagation){ e.stopPropagation(); }
				that.model.collection.remove(that.model);
			}).button()));
			this.$('input[type=text]').input();
			
			new Backbone.ModelBinder().bind(this.model, this.el);
		}
	});
	
	var ObservacoesView = Marionette.CollectionView.extend({
		itemView: ObservacaoView
	});
	
	var LaudoView = COI.FormView.extend({
		template: '#laudo_template',
		regions: {
			'paciente': '#paciente',
			'medico': '#medico',
			'exame': '#exame',
			'observacoes': '#observacoes-list'
		},
		modelEvents: {
			'change:paciente': 'renderPaciente',
			'change:medico': 'renderMedico',
			'change:status': 'updateExame',
			'change:observacoes': 'renderObservacoes'
		},
		ui: {
			'conclusao': '[name=conclusao]',
			'observacoes': '#observacoes',
			'buttonAdicionarObservacao': '#adicionar-observacao'
		},
		triggers: {
			'click #adicionar-observacao': 'adicionarObservacao'
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
			
			this.ui.conclusao.input();
			this.ui.observacoes.input();
			this.ui.buttonAdicionarObservacao.button();
			
			this.renderPaciente();
			this.renderMedico();
			this.renderObservacoes();
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
		renderObservacoes: function() {
			this.observacoes.show(new ObservacoesView({collection: this.model.get('observacoes')}));
		},
		renderExamePreMenopausa: function() {
			this.exame.show(new ExamePreMenopausaView({model: this.model}));
		},
		renderExamePosMenopausa: function() {
			this.exame.show(new ExamePosMenopausaView({model: this.model}));
		},
		onAdicionarObservacao: function(e) {
			var data = this.ui.observacoes.children('option:selected').data();
			this.model.get('observacoes').add(new Observacao({
				codigo: data.code,
				descricao: data.value
			}));
			
			this.ui.observacoes.val(null);
		},
		updateExame: function() {
			switch(this.model.get('status')) {
			case 'Pré-Menopausal':
				this.renderExamePreMenopausa();
				break;
			case 'Transição Menopausal':
			case 'Pós-Menopausal':
				this.renderExamePosMenopausa();
				break;
			}
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