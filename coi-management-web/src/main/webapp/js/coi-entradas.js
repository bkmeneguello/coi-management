"use strict";

COI.module("Entradas", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Entrada = Backbone.Model.extend({
		defaults: {
			data: null,
			paciente: null,
			valor: null,
			tipo: null
		}
	});

	var Entradas = Backbone.Paginator.requestPager.extend({
		url: 'rest/entradas',
		model: Entrada,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: 'rest/entradas'
		},
		paginator_ui: {
			firstPage: 0,
			currentPage: 0
		},
		server_api: {
			'page' : function() {
				return this.currentPage;
			}
		}
	});
	
	var EntradaRowView = COI.ActionRowView.extend({
		template: '#entrada_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('entrada/' + this.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var EntradasView = COI.GridView.extend({
		searchView: COI.SimplePageView,
		itemView: EntradaRowView,
		templateHelpers: {
			header: 'Entradas',
			columns: {
				data: 'Data',
				cliente: 'Cliente',
				valor: 'Valor',
				tipo: 'Tipo'
			}
		},
		extras: {
			'producao': {
				text: 'Produção',
				trigger: 'producao'
			},
			'analitico': {
				text: 'Analítico',
				trigger: 'analitico'
			},
			'fechamentos': {
				text: 'Fechamentos',
				trigger: 'fechamentos'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('entrada', true);
		},
		onProducao: function(e) {
			this.$el.append($('<iframe/>', {'src': 'rest/producao/sintetico'}).hide());
		},
		onAnalitico: function(e) {
			this.$el.append($('<iframe/>', {'src': 'rest/producao/analitico'}).hide());
		},
		onFechamentos: function(e) {
			Backbone.history.navigate('fechamentos', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('entradas', function() {
			COI.body.show(new EntradasView({collection: new Entradas()}));
		});
	});
});